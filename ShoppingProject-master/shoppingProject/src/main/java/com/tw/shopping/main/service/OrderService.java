package com.tw.shopping.main.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tw.shopping.main.dto.CheckoutRequestDto;
import com.tw.shopping.main.entity.CartEntity;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.OrderItemEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.exception.StockNotEnoughException;
import com.tw.shopping.main.repository.CartRepository;
import com.tw.shopping.main.repository.OrderItemRepository;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class OrderService {

    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public OrderEntity createOrder(CheckoutRequestDto request) {
        // 1. 驗證用戶
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("用戶不存在"));

        // 2. 獲取購物車項目
        List<CartEntity> cartItems = cartRepository.findByUserWithProducts(user);
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("購物車為空，無法結帳");
        }

        // 3. 計算總金額
        int totalAmount = cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
        
        // TODO: 這裡可以加上運費計算邏輯
        // totalAmount += 150; 

        // 4. 建立訂單主檔 (OrderEntity)
        OrderEntity order = new OrderEntity();
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(totalAmount);
        
        // 如果是超商取貨，訂單地址可以存 "門市名稱 + 地址"，方便後台查看
        if ("CVS".equals(request.getLogisticsType())) {
             // 格式範例：[7-11 台北店] 台北市信義區...
            String storeInfo = String.format("[%s %s] %s", 
                request.getLogisticsSubType(), 
                request.getStoreName(), 
                request.getAddress()); // 這裡的 address 是前端傳來的門市地址
            order.setOrderAddress(storeInfo);
            
            // 儲存詳細物流欄位 (這些是 Step 1 新增到 Entity 的欄位)
            order.setLogisticsType(request.getLogisticsType());
            order.setLogisticsSubType(request.getLogisticsSubType());
            order.setStoreId(request.getStoreId());
            order.setStoreName(request.getStoreName());
            order.setStoreAddress(request.getAddress());
            
        } else {
            // 一般宅配
            order.setOrderAddress(request.getAddress());
            order.setLogisticsType("HOME");
            order.setLogisticsSubType("TCAT"); // 預設黑貓或宅配通
        }

        order.setPaymentstatus("PENDING"); 
        order.setUserid(user); 
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentmethods("1"); // 假設 1 是信用卡
        
        
        order.setOrderAddress(request.getAddress());
        order.setPaymentstatus("PENDING"); // 初始狀態
        order.setUserid(user); 
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentmethods("1");
        
        // 先儲存 Order 以取得 OrderId
        OrderEntity savedOrder = orderRepository.save(order);

        // 防止死鎖，所以在進入迴圈前，強制將購物車項目依照 Product ID 排序
        cartItems.sort(Comparator.comparing(item -> item.getProduct().getProductid()));

        // 5. 建立訂單明細 (OrderItemEntity) & ★ 處理庫存鎖定 (這是我臣又貝修改的地方)
        List<OrderItemEntity> orderItems = new ArrayList<>();
        
        for (CartEntity cartItem : cartItems) {
            // 接下來是加上原子更新的修改後的程式碼
            // 1. 獲取商品ID
            Integer productid = cartItem.getProduct().getProductid();
            Integer buyQuantity = cartItem.getQuantity();

            // 🔥【關鍵修改】直接嘗試在資料庫扣庫存
            // SQL: UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?
            int updateCount = productRepository.decreaseStock(productid, buyQuantity);
            
            if (updateCount == 0) {
                // 如果回傳 0，代表 WHERE 條件不成立 (stock < buyQuantity)，也就是庫存不足
                // 拋出異常，觸發 @Transactional 全部回滾
                throw new StockNotEnoughException("商品 [" + cartItem.getProduct().getPname() + "] 庫存不足，無法結帳！");
            }

            // --- 執行到這裡代表庫存已經扣成功了 ---

            // 因為 decreaseStock 只是執行 SQL，沒有回傳 Entity，
            // 所以我們需要重新讀取一次商品資訊來建立訂單明細
            // (這時候讀到的庫存會是扣除後的，但沒關係，訂單明細重點是價格)
            ProductEntity product = productRepository.findById(productid)
                    .orElseThrow(() -> new RuntimeException("商品異常消失"));

            // 2. 建立訂單明細 (這裡建議使用 lockedProduct，確保資料最新)
            // 先前的我是使用 cartItem.getProduct()，但那可能不是最新的庫存資料
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(savedOrder); // 設定關聯
            orderItem.setProductId(product.getProductid().longValue());
            orderItem.setQuantity(buyQuantity);
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setDiscount(java.math.BigDecimal.ZERO); // 預設無折扣
            orderItem.setProduct(product); // 設定商品關聯
            
            orderItems.add(orderItem);
        }
        
        // 批次儲存明細
        orderItemRepository.saveAll(orderItems);
        
        // 將明細設回訂單物件 (為了後續 ECPay 顯示商品名稱)
        savedOrder.setOrderItems(orderItems);

        // 6. 清空購物車
        cartRepository.deleteAll(cartItems);

        return savedOrder;
    }
}