package com.tw.shopping.main.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//用於前端傳入的加入購物車請求
@Data
public class AddToCartDto {
	@NotNull(message = "用戶ID不能為空")
    private Long userid; // 關聯 userinfo.userid
    
    @NotNull(message = "商品ID不能為空")
    private Integer productid; // 關聯 product.productid
    
    @Min(value = 1, message = "購買數量至少為 1")
    private Integer quantity;
 
}
