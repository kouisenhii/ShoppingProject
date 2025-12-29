package com.tw.shopping.main.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.AddToCartDto;
import com.tw.shopping.main.dto.CartItemView;
import com.tw.shopping.main.dto.CartResponseDto;
import com.tw.shopping.main.dto.UpdateQuantityDto;
import com.tw.shopping.main.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/cart")
@Tag(name = "購物車", description = "新增、刪除購物車等等")
public class CartController {
	@Autowired
	private  CartService cartService;

	
	@PostMapping("/add")
	@Operation(summary = "新增商品至購物車", description = "根據id新增商品至購物車")
    public ResponseEntity<CartResponseDto> addItemToCart(@Valid @RequestBody AddToCartDto request) {
        try {
            // 調用 Service 處理 Upsert 邏輯
            CartResponseDto responseDto = cartService.addToCartAndGetResponse(request);
            
            // 返回新增或更新後的購物車項目
            return ResponseEntity.ok(responseDto);
            
        }catch(RuntimeException e){
            // 建立一個失敗的 DTO 物件
            CartResponseDto errorResponse = new CartResponseDto();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage()); // 把錯誤訊息塞進去
            
            // 回傳 400，但 body 依然是 CartResponseDto 格式
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/count/{userId}")
    @Operation(summary = "計算購物車數量", description = "計算該userid內的購物車數量")
    public Long getCartItemCount(@PathVariable Long userId) {
        return cartService.getTotalCartQuantity(userId);
    }
    
    // 購物車頁面拉資料
    @GetMapping("/{userId}")
    @Operation(summary = "購物車頁面抓資料", description = "抓取當前的購物車資料")
    public List<CartItemView> getCart(@PathVariable Long userId) {
    		return cartService.getCartItems(userId);
    }
    
   // 更新購物車項目數量
    @PutMapping("/quantity") // 使用 PUT 更符合 RESTful 語義
    @Operation(summary = "更新購物車數量", description = "更新購物車數量至資料庫")
    public ResponseEntity<String> updateCartItemQuantity(@Valid @RequestBody UpdateQuantityDto request) {
        try {
            // 調用 Service 處理更新邏輯
            cartService.updateQuantity(request.getCartId(), request.getQuantity());
            
            // 返回成功訊息
            return ResponseEntity.ok("購物車項目數量更新成功");
        } catch (IllegalArgumentException e) {
            // 處理數量 <= 0 的情況
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            // 處理找不到項目等一般錯誤
            return ResponseEntity.status(404).body("更新失敗: " + e.getMessage());
        }
    }
    
 // 新增：刪除購物車項目
    @DeleteMapping("/{cartId}") // 範例：DELETE /api/cart/123
    @Operation(summary = "刪除購物車", description = "刪除購物車內的商品")
    public ResponseEntity<String> removeCartItem(@PathVariable Long cartId) {
        try {
            // 調用 Service 處理刪除邏輯
            cartService.removeItem(cartId);
            
            // 返回成功訊息
            return ResponseEntity.ok("購物車項目刪除成功: " + cartId);
        } catch (RuntimeException e) {
            // 處理找不到項目或其他錯誤
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
