package com.tw.shopping.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
	
	private Integer productId;   // 關聯的商品 ID
	private String productName; // 假設需要展示商品名稱
    private Integer quantity;    // 購買數量
    private Integer unitPrice;   // 單價
    private Integer subTotal;    //小計
    
}
