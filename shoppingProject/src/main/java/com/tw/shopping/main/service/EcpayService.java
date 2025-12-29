package com.tw.shopping.main.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 確保有這個 import

import com.tw.shopping.main.config.EcpayProperties;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.OrderItemEntity;
import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.util.EcpayUtil;

// @Transactional 可以加在 Class 上，但為了精確控制，我們只加在需要的方法上
@Service
public class EcpayService {

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private OrderRepository orderRepository;
    
    
    // 定義綠界使用的日期格式
    private static final String ECPAY_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /**
     * 輔助方法：將綠界回傳的日期字串轉換為 Date 物件
     */
    private Date parseEcpayDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // 使用 yyyy/MM/dd HH:mm:ss 格式
        SimpleDateFormat formatter = new SimpleDateFormat(ECPAY_DATE_FORMAT, Locale.TAIWAN);
        try {
            return formatter.parse(dateStr);
        } catch (ParseException e) {
            System.err.println("❌ ECPay Date 轉換失敗: " + dateStr + "，錯誤: " + e.getMessage());
            return null;
        }
    }


    /**
     * 根據訂單資訊，準備綠界金流 AIO CheckOut 的參數，並計算 CheckMacValue。
     * @param order 剛建立並儲存到資料庫的 OrderEntity
     * @return 綠界所需的參數 Map
     */
    // 【關鍵修正】：加上 @Transactional 以開啟 JPA Session，允許 Lazy Loading
    @Transactional 
    public Map<String, String> prepareCheckoutForm(OrderEntity order) {
        
        // 【關鍵修正】：重新載入 OrderEntity
        // 確保我們操作的是一個與當前 @Transactional Session 綁定的 Managed Entity
        OrderEntity managedOrder = orderRepository.findById(order.getOrderId()).orElseThrow(
            () -> new RuntimeException("ECPay 處理：找不到訂單 ID: " + order.getOrderId())
        );
        
        // 1. 準備商品名稱字串 (綠界 ItemName)
        // 格式: 商品A x 數量A#商品B x 數量B
        // 這裡調用 managedOrder.getOrderItems() 會安全地觸發 Lazy Loading
        String itemName = managedOrder.getOrderItems().stream()
            .map(item -> {
                // item.getProduct() 現在應該不會是 null
                return item.getProduct().getPname() + " x " + item.getQuantity();
            })
            .collect(Collectors.joining("#"));

        // 2. 設定綠界交易參數
        Map<String, String> ecpayParams = new HashMap<>();
        
        // 從 application.properties 讀取
        ecpayParams.put("MerchantID", ecpayProperties.getMerchantId()); 
        
        // 訂單編號 (唯一性)：使用 OrderId 加上時間戳確保唯一，並符合綠界 20 碼限制
        // 這裡使用 managedOrder.getOrderId()
        String merchantTradeNo = "TW" + managedOrder.getOrderId() + System.currentTimeMillis() % 10000;
        ecpayParams.put("MerchantTradeNo", merchantTradeNo);
        
        // 交易時間 (yyyy/MM/dd HH:mm:ss)
        String tradeDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern(ECPAY_DATE_FORMAT));
        ecpayParams.put("MerchantTradeDate", tradeDate);
        
        // 交易總金額
        ecpayParams.put("TotalAmount", managedOrder.getTotalAmount().toString());
        
        // 商品名稱 (已在上面處理 ItemName)
        ecpayParams.put("ItemName", itemName);
        
        // 交易描述 (可選)
        ecpayParams.put("TradeDesc", "線上購物結帳");
        
        // 付款完成後導向的網址
        String baseCallbackUrl = ecpayProperties.getNgrokBaseUrl(); // 從您的 properties 讀取
        
        // 綠界回傳給您的後端網址 (接收付款結果)
        ecpayParams.put("ReturnURL", baseCallbackUrl + "/api/ecpay/callback"); 
        
        // 付款完成後，使用者被導回的網址 (GET 請求)
        ecpayParams.put("ClientBackURL", baseCallbackUrl + "/index.html");
        
        // 選擇交易模式 (ALL 表示所有付款方式)
        ecpayParams.put("ChoosePayment", "ALL"); 
        
        // 交易類型，固定 AIO
        ecpayParams.put("PaymentType", "aio");
        
        // 隱藏欄位 (固定)
        ecpayParams.put("EncryptType", "1");
        ecpayParams.put("Language", "CHT");

        // 3. 計算 CheckMacValue (簽章值)
        String checkMacValue = EcpayUtil.generateCheckMacValue(ecpayParams, 
                                                               ecpayProperties.getHashKey(), 
                                                               ecpayProperties.getHashIV());

        ecpayParams.put("CheckMacValue", checkMacValue);

        // 4. 更新訂單中的 MerchantTradeNo (用於綠界回傳時的比對)
        managedOrder.setEcpaytradeno(merchantTradeNo);
        orderRepository.save(managedOrder); // 更新訂單
        
        return ecpayParams;
    }
    
    /**
     * 驗證綠界回傳參數的 CheckMacValue
     */
    public boolean verifyCheckMacValue(Map<String, String> params) {
        String receivedCheckMacValue = params.get("CheckMacValue");
        
        // 綠界規定：回傳時需移除 CheckMacValue 欄位後再計算
        Map<String, String> cleanParams = new HashMap<>(params);
        cleanParams.remove("CheckMacValue"); 

        String calculatedCheckMacValue = EcpayUtil.generateCheckMacValue(
            cleanParams, 
            ecpayProperties.getHashKey(), 
            ecpayProperties.getHashIV()
        );

        // 比較計算結果與收到的結果
        return calculatedCheckMacValue.equals(receivedCheckMacValue);
    }

    /**
     * 處理付款成功通知
     * @param merchantTradeNo 綠界傳回來的特店交易編號 (例如 TW10138708)
     * @param ecpayResult 綠界回傳的完整參數 Map
     */
    @Transactional
    public void handlePaymentSuccess(String merchantTradeNo, Map<String, String> ecpayResult) {
        
        // 1. 【核心修正】直接用交易編號字串查詢，不再進行 Long.parseLong() 轉換
        OrderEntity order = orderRepository.findByEcpaytradeno(merchantTradeNo)
                .orElseThrow(() -> new RuntimeException("找不到訂單，綠界交易編號: " + merchantTradeNo));
        
        System.out.println("處理成功訂單，資料庫 ID: " + order.getOrderId());

        // 2. 判斷是否為重複通知 (避免重複處理)
        if ("PAID".equals(order.getPaymentstatus())) {
            System.out.println("訂單 " + order.getOrderId() + " 已支付過，忽略重複通知。");
            return; 
        }

        // 3. 更新訂單狀態和綠界回傳資訊
        order.setPaymentstatus("PAID"); // 狀態改為 PAID
        order.setOrderStatus(OrderStatus.PAID);
        
        // 【新增】更新具體的付款方式 (例如: Credit_CreditCard)
        // 這裡直接把綠界回傳的英文代號存進去，最準確
        String paymentType = ecpayResult.get("PaymentType");
        order.setPaymentmethods(paymentType);
        
        // 解析付款時間
        String paymentDateStr = ecpayResult.get("PaymentDate");
        if (paymentDateStr != null) {
            order.setPaymentconfirmdate(parseEcpayDate(paymentDateStr));
        }
        
        // 更新綠界回傳的其他資訊
        order.setEcpaytradeno(ecpayResult.get("TradeNo")); // 綠界那邊的流水號 (不同於 MerchantTradeNo)
        order.setEcpayrtncode(ecpayResult.get("RtnCode"));
        order.setEcpayrtnmsg(ecpayResult.get("RtnMsg"));

        // 4. 儲存變更
        orderRepository.save(order);
        System.out.println("DB 訂單 " + order.getOrderId() + " 已更新為 PAID。");
    }

    /**
     * 處理付款失敗通知
     */
    @Transactional
    public void handlePaymentFailure(String merchantTradeNo, Map<String, String> ecpayResult) {
        
        // 1. 【核心修正】一樣改用 findByEcpaytradeno
        OrderEntity order = orderRepository.findByEcpaytradeno(merchantTradeNo)
             .orElseThrow(() -> new RuntimeException("找不到訂單，綠界交易編號: " + merchantTradeNo));
             
        // 2. 取得失敗原因
        String rtnMsg = ecpayResult.getOrDefault("RtnMsg", "交易失敗，無詳細訊息");
        String rtnCode = ecpayResult.getOrDefault("RtnCode", "N/A");
        
        System.out.println("處理失敗訂單 ID: " + order.getOrderId() + ", 原因: " + rtnMsg);
        
        // 3. 更新訂單狀態
        order.setPaymentstatus("FAILED"); 
        order.setEcpayrtncode(rtnCode);
        order.setEcpayrtnmsg(rtnMsg);
        
        orderRepository.save(order);
        System.out.println("DB 訂單 " + order.getOrderId() + " 已更新為 FAILED。");
    }
    
 // ----------------------------------------------------------------
    // 【新增】綠界物流地圖相關 (Start)
    // ----------------------------------------------------------------

    // 綠界物流地圖查詢 URL (測試環境)
    private static final String ECPAY_MAP_URL = "https://logistics-stage.ecpay.com.tw/Express/map";

    /**
     * 產生綠界電子地圖 (CVS Map) 的 Form 表單 HTML
     * @param logisticsSubType 超商類型: UNIMART(7-11), FAMI(全家), HILIFE(萊爾富), OKMART(OK)
     * @return HTML Form 字串 (包含自動 submit 的 script)
     */
    public String prepareLogisticsMapForm(String logisticsSubType) {
        Map<String, String> mapParams = new HashMap<>();

        // 1. 基本參數
        mapParams.put("MerchantID", ecpayProperties.getMerchantId());
        mapParams.put("MerchantTradeNo", "Map" + System.currentTimeMillis()); // 每次隨機產生即可，地圖查詢不需紀錄
        mapParams.put("LogisticsType", "CVS"); // 固定 CVS
        mapParams.put("LogisticsSubType", logisticsSubType); // 使用者選擇的超商 (UNIMART, FAMI...)
        mapParams.put("IsCollection", "N"); // 是否代收貨款 (N: 純取貨, Y: 取貨付款)，地圖階段通常選 N 即可
        
        // 2. 回傳網址 (ServerReplyURL)
        // 【重要】這是使用者選完門市後，綠界 Server POST 資料回來的網址
        // 必須是外網可存取的網址 (如 ngrok)
        // 如果是在本機開發且沒有 ngrok，綠界無法呼叫到這裡。
        String localBaseUrl = "http://localhost:8080";

//        mapParams.put("ServerReplyURL", localBaseUrl + "/api/ecpay/map-callback");
        mapParams.put("ServerReplyURL", ecpayProperties.getNgrokBaseUrl() + "/api/ecpay/map-callback");

        // 3. 組裝 HTML Form 自動送出
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body>");
        html.append("<form id='ecpay-map-form' action='").append(ECPAY_MAP_URL).append("' method='POST'>");
        
        for (Map.Entry<String, String> entry : mapParams.entrySet()) {
            html.append("<input type='hidden' name='").append(entry.getKey()).append("' value='").append(entry.getValue()).append("' />");
        }
        
        html.append("</form>");
        html.append("<script>document.getElementById('ecpay-map-form').submit();</script>");
        html.append("</body></html>");

        return html.toString();
    }
    // ----------------------------------------------------------------
    // 【新增】綠界物流地圖相關 (End)
    // ----------------------------------------------------------------
}