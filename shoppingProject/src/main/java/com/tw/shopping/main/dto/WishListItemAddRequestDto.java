package com.tw.shopping.main.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishListItemAddRequestDto {
	
	@NotBlank(message = "商品ID不可為空")
	private Integer productId;
}
