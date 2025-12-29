package com.tw.shopping.main.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.AddToCartDto;
import com.tw.shopping.main.dto.CartItemView;
import com.tw.shopping.main.dto.CartResponseDto;
import com.tw.shopping.main.entity.CartEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.CartRepository;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.repository.UserRepository;

@Service
public class CartService {

    private final CategoryRepository categoryRepository;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProductRepository productRepository;

    CartService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

   @org.springframework.transaction.annotation.Transactional
    public CartResponseDto addToCartAndGetResponse(AddToCartDto request) { 
        // 1. 驗證並獲取關聯實體 (保持不變)
        UserEntity user = userRepository.findById(request.getUserid())
            .orElseThrow(() -> new RuntimeException("用戶不存在"));
            
        ProductEntity product = productRepository.findById(request.getProductid())
            .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 這是防止超賣的最後一道防線
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("商品庫存不足，無法加入購物車");
        }

        // 2. 檢查購物車中是否已存在該項目 (保持不變)
        Optional<CartEntity> existingCartItem = cartRepository.findByUserAndProduct(user, product);
        
        CartEntity savedCartItem;
        
        if (existingCartItem.isPresent()) {
            // 情況 A: 項目已存在 (UPDATE)
            CartEntity cartItem = existingCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setAddtime(LocalDateTime.now()); 
            savedCartItem = cartRepository.save(cartItem); // 執行更新
            
        } else {
            // 情況 B: 項目不存在 (INSERT)
            CartEntity newCartItem = new CartEntity(user, product, request.getQuantity());
            savedCartItem = cartRepository.save(newCartItem); // 執行新增
        }

        // 將結果映射到 CartResponseDto
        CartResponseDto response = new CartResponseDto();
        response.setSuccess(true);
        response.setMessage("商品已成功加入購物車，數量更新為 " + savedCartItem.getQuantity());
        response.setCartid(savedCartItem.getCartid());
        
        // 關鍵：從請求 DTO 中獲取 productid，因為 CartEntity 的 product 欄位被 @JsonIgnore 忽略了
        response.setProductid(request.getProductid()); 
        response.setQuantity(savedCartItem.getQuantity());
        
        return response; // 回傳 DTO
    }

    public Long getTotalCartQuantity(Long userId) {
        // 1. 驗證用戶是否存在
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用戶不存在"));
            
        
        // Long totalQuantity = cartRepository.countTotalItemsByUserId(userId);
        
        // 為了提供一個可以直接運行的範例，我們假設您有一個遍歷方法:
        Long totalQuantity = cartRepository.findByUser(user)
                                .stream()
                                .mapToLong(CartEntity::getQuantity)
                                .sum();
        
        return totalQuantity != null ? totalQuantity : 0L;
    }
    
    // 購物車邏輯
    public List<CartItemView> getCartItems(Long userId) {
    		return cartRepository.findCartItemByUserId(userId);
    }
    
    // 新增：更新購物車項目數量
    @org.springframework.transaction.annotation.Transactional
    public void updateQuantity(Long cartId, int quantity) {
        // 1. 根據 cartId 查找購物車項目
        Optional<CartEntity> optionalCart = cartRepository.findById(cartId); 

        if (optionalCart.isPresent()) {
            CartEntity cartItem = optionalCart.get();
            
            // 2. 判斷邏輯：如果數量 <= 0，則刪除該項目
            if (quantity <= 0) {
                cartRepository.delete(cartItem); // 執行刪除
                return; // 刪除成功後直接返回
            } 
            
            // 3. 數量 > 0: 更新數量
            cartItem.setQuantity(quantity); // 設定新數量
            cartItem.setAddtime(LocalDateTime.now()); // 更新時間戳 (可選)
            cartRepository.save(cartItem); // 儲存更新
            
        } else {
            throw new RuntimeException("找不到 Cart ID: " + cartId);
        }
    }
    
    
 // 新增：根據 cartId 刪除購物車項目
    @org.springframework.transaction.annotation.Transactional
    public void removeItem(Long cartId) {
        // 1. 查找項目確保存在 (可選，直接 deleteById 也可以，但查找可以拋出更精確的錯誤)
        Optional<CartEntity> optionalCart = cartRepository.findById(cartId); 

        if (optionalCart.isPresent()) {
            // 2. 執行刪除
            cartRepository.delete(optionalCart.get()); 
        } else {
            // 如果找不到，拋出異常
            throw new RuntimeException("找不到 Cart ID: " + cartId + "，無法刪除");
        }
    }
    
    
}