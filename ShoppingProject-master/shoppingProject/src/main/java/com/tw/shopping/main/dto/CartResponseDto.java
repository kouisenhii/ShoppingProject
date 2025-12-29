package com.tw.shopping.main.dto;
import lombok.Data;
@Data
public class CartResponseDto {
    private boolean success = true;  // 標示成功
    private String message;          // 提示訊息
    private Long cartid;             // 購物車項目 ID
    private Integer productid;       // 直接包含 productid
    private Integer quantity;        // 該項目的最新數量
   
}

/*
 *  前端需要 {
 *  				userid:
 *  				address:
 *  				cartid:
 *  				quantity:
 *  				productid:
 *  				pname:
 *  				description:
 *  				price:
 *  				productimage:
 *  				color:
 *  				specification:
 *  			}
 */