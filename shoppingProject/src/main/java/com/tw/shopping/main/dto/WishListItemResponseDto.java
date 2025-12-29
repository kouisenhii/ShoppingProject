package com.tw.shopping.main.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishListItemResponseDto {
	
	private Integer productId;
	private String productName;
	private String productImage;
	private String description;
	private String color;
	private String specification;
	private Integer price;
	private LocalDate addTime;
	
	//圖片、敘述、顏色、規格
	

}
