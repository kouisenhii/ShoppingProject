package com.tw.shopping.main.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.repository.CartRepository;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.ProductRepository;

@org.springframework.stereotype.Service
public class HomeService {
	
	
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private CartRepository cartRepository;
	
	private final List<Integer> SEVEN_PRODUCT_IDS = Arrays.asList(470, 358, 1305, 4, 2, 1178, 17);
	private final List<Integer> PD_PRODUCT_IDS = Arrays.asList(1281, 1282, 1283);
	
	// 本月主打 七筆資料
    // 快取名稱: home_seven_products
	@Cacheable(value = "home_seven_products", unless = "#result == null") 
	public List<ProductEntity> getSevenProduct() {
        System.out.println("快取未命中：查詢本月主打商品..."); // 測試用，上線可拿掉
		return productRepository.findAllById(SEVEN_PRODUCT_IDS);
	}

	// 推薦熱點系列資料
    // 快取名稱: home_featured_products
	@Cacheable(value = "home_featured_products", unless = "#result == null")
	public List<ProductEntity> getpdProduct() {
        System.out.println("快取未命中：查詢推薦熱點商品...");
		return productRepository.findAllById(PD_PRODUCT_IDS);
	}
	
	/**
	 * 取得所有產品資料 (包含本月主打和推薦熱點)
     * 快取名稱: home_all_products
	 */
	@Cacheable(value = "home_all_products", unless = "#result == null")
	public List<ProductEntity> getAllProductsData() {
        System.out.println("快取未命中：查詢所有首頁商品...");
		// 結合兩個 ID 列表並去重，然後一次性查詢資料庫
		List<Integer> allIds = Stream.concat(SEVEN_PRODUCT_IDS.stream(), PD_PRODUCT_IDS.stream())
                .distinct()
                .collect(Collectors.toList());
		
		return productRepository.findAllById(allIds);
	}
	
    // 產品分類
    // 快取名稱: home_categories
	@Cacheable(value = "home_categories", unless = "#result == null")
	public List<CategoryEntity> getCategory() {
        System.out.println("快取未命中：查詢分類...");
		List<Integer> ids = Arrays.asList(1, 2, 3, 4);
		return categoryRepository.findAllById(ids);
	}
}