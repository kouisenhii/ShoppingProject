package com.tw.shopping.main.controller;

import org.springframework.web.bind.annotation.*;

import com.tw.shopping.main.config.EcpayProperties;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.service.EcpayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ecpay")
@Tag(name = "綠界API", description = "串聯綠界物流、金流")
public class EcpayController {

    @Autowired
    private EcpayService ecpayService;
    
    @Autowired
    private EcpayProperties ecpayProperties;

    // 2. 注入 OrderRepository
    @Autowired
    private OrderRepository orderRepository; 

    @PostMapping("/checkout/{orderId}")
    @Operation(summary = "抓取訂單", description = "抓取訂單資料")
    public Map<String, String> checkout(@PathVariable("orderId") Integer orderId) {
        
        // 3. 【修正】：從資料庫讀取真實訂單，而不是 new 一個假的
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到訂單 ID: " + orderId));

        // 4. 傳入真實的 order 物件給 Service
        Map<String, String> ecpayParams = ecpayService.prepareCheckoutForm(order);
        
    
        
        return ecpayParams;
    }
    
    
    /**
     * [POST /api/ecpay/callback] 
     * 接收綠界支付結果的幕後通知 (ReturnURL)
     * * @param allParams 綠界 POST 回傳的所有參數 (使用 HashMap 接收所有 @RequestParam)
     * @return 必須回傳 "1|OK" 給綠界，否則綠界會不斷重送
     */
    @PostMapping("/callback")
    @Transactional // 確保資料庫更新成功後才回覆
    @Operation(summary = "回傳訂單", description = "回傳綠界訂單明細")
    public ResponseEntity<String> ecpayCallback(@RequestParam Map<String, String> allParams) {
        
        // 1. 進行 CheckMacValue 驗證 (安全核心!)
        boolean isValid = ecpayService.verifyCheckMacValue(allParams);
        
        if (!isValid) {
            System.out.println("【ECPay 驗證失敗】: HashMacValue 不符，拒絕處理!");
            return ResponseEntity.badRequest().body("0|Hash Check Failed");
        }
        
        // 2. 驗證成功，處理訂單狀態更新
        String orderId = allParams.get("MerchantTradeNo");
        String rtnCode = allParams.get("RtnCode");
        
        if ("1".equals(rtnCode)) {
            // 付款成功
            System.out.println("【ECPay 成功通知】: 訂單 " + orderId + " 付款成功，準備更新DB。");
            ecpayService.handlePaymentSuccess(orderId, allParams); // 處理DB更新邏輯
        } else {
            // 付款失敗或處理中
            System.out.println("【ECPay 失敗通知】: 訂單 " + orderId + " 交易失敗，RtnCode: " + rtnCode);
            ecpayService.handlePaymentFailure(orderId, allParams); // 處理失敗邏輯
        }
        
        // 3. 必須回傳 "1|OK" 給綠界
        return ResponseEntity.ok("1|OK");
    }
    
 // ----------------------------------------------------------------
    // 【新增】綠界物流地圖相關 API (Start)
    // ----------------------------------------------------------------

    /**
     * [Step 1] 前端呼叫此 API，取得綠界地圖 Form 並自動跳轉
     * @param logisticsSubType 超商類型 (UNIMART, FAMI...)
     */
    @PostMapping("/map")
    @Operation(summary = "呼叫綠界金流", description = "呼叫並跳轉到綠界API")
    public void goToMap(@RequestParam("logisticsSubType") String logisticsSubType, 
                        HttpServletResponse response) throws IOException {
        // 取得自動跳轉的 HTML
        String formHtml = ecpayService.prepareLogisticsMapForm(logisticsSubType);
        
        // 直接寫入 Response，讓瀏覽器執行 HTML 中的 JS 進行跳轉
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(formHtml);
    }

    /**
     * [Step 2] 綠界選完門市後，POST 回來的 Callback
     * 這裡負責接收門市資訊，並將使用者導回購物車頁面 (帶參數)
     */
    @PostMapping("/map-callback")
    @Operation(summary = "回傳綠界物流資訊", description = "回傳綠界物流的超商資訊(測試環境門市是固定的)")
    public void mapCallback(@RequestParam Map<String, String> params, 
                            HttpServletResponse response) throws IOException {
        
        // 1. 取得綠界回傳的門市資訊
        String storeId = params.get("CVSStoreID");       // 門市代號
        String storeName = params.get("CVSStoreName");   // 門市名稱
        String storeAddress = params.get("CVSAddress");  // 門市地址
        String logisticsSubType = params.get("LogisticsSubType"); // 超商類型
        
        System.out.println("【ECPay Map】使用者選擇門市: " + storeName + " (" + storeId + ")");

        // 2. 處理中文編碼 (避免 URL 亂碼)
        // 注意：storeName 和 storeAddress 可能包含中文
        String encodedStoreName = URLEncoder.encode(storeName, StandardCharsets.UTF_8);
        String encodedAddress = URLEncoder.encode(storeAddress, StandardCharsets.UTF_8);

        // 3. 重新導向回前端購物車頁面，並將資訊帶在 URL 參數中
        // 假設您的購物車頁面是 /cart.html
        String redirectUrl = String.format("/html/cart.html?storeId=%s&storeName=%s&address=%s&type=%s",
                storeId, encodedStoreName, encodedAddress, logisticsSubType);
        
        response.sendRedirect(redirectUrl);
    }

    // ----------------------------------------------------------------
    // 【新增】綠界物流地圖相關 API (End)
    // ----------------------------------------------------------------
    
}
