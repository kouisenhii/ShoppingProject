package com.tw.shopping.main.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ProductDetailDto implements Serializable{
	private static final long serialVersionUID = 1L;

	private Integer productid;
	private String pname, description,specification, productimage, color, shipping;
	private Integer price, stock, categoryid;
	private List<RatingDto> review;
	private Options options;
    private Images images;
	private String mainCategoryCode;
	private String mainCategoryName;
	private String subCategoryCode;
	private String subCategoryName;
	private Double averageRating;	//平均分數
	private Integer reviewCount;	//評論總數
	



	//-----------------------------------
	@Data
	public static class Options implements Serializable{
		private static final long serialVersionUID = 1L;

        private List<ColorOption> color;
    }

	@Data
	public static class ColorOption implements Serializable{
		private static final long serialVersionUID = 1L;

        private Integer productid;
        private String value;
        private String name;
        private String specification;
        private Integer price;
        private Integer stock;
    }

	@Data
	public static class Images implements Serializable{
		private static final long serialVersionUID = 1L;

        private List<String> main;
        private List<String> thumb;
    }

	@Data
	public static class RatingDto implements Serializable{
		private static final long serialVersionUID = 1L;

        private String color;
        private Integer rating;
    }

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RelatedProductDto implements Serializable{
		private static final long serialVersionUID = 1L;

		private Integer productid;
		private String description;
		private String pname;
		private Integer price;
		private String productimage;
	}
	@Data
	public static class ReviewPageDto implements Serializable{
		private static final long serialVersionUID = 1L;
		
		private List<RatingDto> reviews;	// 當前頁面的評論列表
		private Integer currentPage;		// 目前第幾頁
		private Integer totalPages;			// 總頁數
		private Long totalElements;			// 總評論數
	}
	
}
