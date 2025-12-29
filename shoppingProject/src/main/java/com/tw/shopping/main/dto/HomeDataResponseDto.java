package com.tw.shopping.main.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 統一首頁資料回應的 DTO
 */
@Data
public class HomeDataResponseDto implements Serializable{
	private static final long serialVersionUID = 1L; // 版本號
    
    // 對應本月主打產品
    private List<ProductDto> mainProducts;
    
    // 對應推薦熱點產品
    private List<ProductDto> featuredProducts;
    
    // 對應產品系列
    private List<CategoryDto> categories;
}