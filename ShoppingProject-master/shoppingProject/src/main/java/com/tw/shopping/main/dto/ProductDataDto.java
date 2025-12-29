package com.tw.shopping.main.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProductDataDto {
	    
	  private List<ProductDto> featuredProducts; // 推薦熱點系列
	  private List<ProductDto> mainProducts;     // 本月主打

	    // 您可以根據需要新增 Getter/Setter 或使用 Lombok 的 @Data
}

