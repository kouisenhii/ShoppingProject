package com.tw.shopping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.tw.shopping.main.dto.CheckoutRequestDto;
import com.tw.shopping.main.entity.CartEntity;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.CartRepository;
import com.tw.shopping.main.repository.OrderItemRepository;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.service.OrderService;

@SpringBootTest
public class OversellTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderItemRepository orderItemRepository;

    private final Integer TEST_PRODUCT_ID = 1;

    @BeforeEach
    public void setup() {
        // 1. 初始化庫存 (真實 DB 操作)
        transactionTemplate.execute(status -> {
            ProductEntity product = productRepository.findById(TEST_PRODUCT_ID).orElse(null);
            if (product == null) {
                 throw new RuntimeException("Setup 失敗：資料庫必須先存在 ID=" + TEST_PRODUCT_ID + " 的商品，請檢查 DB 連線或資料");
            }
            product.setStock(10); 
            product.setPrice(100);
            productRepository.save(product);
            return null;
        });

        // ⭐ Debug: 確認 Setup 後 DB 裡的庫存真的是 10
        ProductEntity check = productRepository.findById(TEST_PRODUCT_ID).orElseThrow();
        System.out.println(">>> Setup 完成，目前 DB 商品 ID=" + TEST_PRODUCT_ID + " 庫存為: " + check.getStock());

        // 2. Mock 用戶
        UserEntity mockUser = new UserEntity();
        mockUser.setUserid(1L); 
        when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

        // 3. Mock 購物車
        List<CartEntity> mockCart = new ArrayList<>();
        CartEntity cartItem = new CartEntity();
        cartItem.setQuantity(1);
        
        ProductEntity productInCart = new ProductEntity();
        productInCart.setProductid(TEST_PRODUCT_ID);
        productInCart.setPrice(100);
        productInCart.setPname("測試商品");
        cartItem.setProduct(productInCart);
        mockCart.add(cartItem);

        when(cartRepository.findByUserWithProducts(any())).thenReturn(mockCart);
        
        // 4. Mock 訂單存檔 (確保使用 OrderEntity.class 匹配)
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testConcurrentOrder_AtomicUpdate() throws InterruptedException {
        int numberOfThreads = 50; 
        int initialStock = 10;   

        CheckoutRequestDto request = new CheckoutRequestDto();
        request.setUserId(1L); 
        request.setAddress("Test Address");
        request.setLogisticsType("HOME");
        request.setLogisticsSubType("TCAT");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    startLatch.await(); 
                    orderService.createOrder(request);
                    successCount.incrementAndGet(); 
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    
                    // ⭐ 強力 Debug: 無論什麼錯誤都印出來，並包含 Exception 類型
                    // 這樣我們才知道是 NPE 還是 SQL 錯誤
                    System.out.println("❌ 搶購失敗 [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
                    if (!(e instanceof RuntimeException && e.getMessage() != null && e.getMessage().contains("庫存不足"))) {
                         e.printStackTrace(); // 如果不是預期的庫存不足，印出堆疊
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); 
        endLatch.await(); 

        ProductEntity finalProduct = productRepository.findById(TEST_PRODUCT_ID).orElseThrow();
        
        System.out.println("==============================================");
        System.out.println("原子更新並發測試結果：");
        System.out.println("初始庫存: " + initialStock);
        System.out.println("搶購人數: " + numberOfThreads);
        System.out.println("成功訂單: " + successCount.get());
        System.out.println("失敗請求: " + failCount.get());
        System.out.println("剩餘庫存: " + finalProduct.getStock());
        System.out.println("==============================================");

        assertEquals(0, finalProduct.getStock());
        assertEquals(initialStock, successCount.get());
        assertEquals(numberOfThreads - initialStock, failCount.get());
    }
}