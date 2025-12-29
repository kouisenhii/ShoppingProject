package com.tw.shopping.main.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class UpdateQuantityDto {
	// 購物車項目ID (對應 shopping.cart.cartid)
    @NotNull(message = "購物車項目ID不能為空")
    private Long cartId; 
    
    // 新的數量
    private int quantity;
}
