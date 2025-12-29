package com.tw.shopping.main.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequestDto {
	
	@Size(min = 6 , max = 255, message = "最少需填入6字")
	@Pattern( 
		regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s]{6,255}$",
	    	message = "地址必須包含中文，且長度在6到255個字元之間，可包含數字、英文")
	private String orderAddress;
    
	

}
