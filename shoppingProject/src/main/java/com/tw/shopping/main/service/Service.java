package com.tw.shopping.main.service;

import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;


import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.repository.CartRepository;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.ProductRepository;

@org.springframework.stereotype.Service
public class Service {
	
	
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private CartRepository cartRepository;
	
	// 本月主打的產品 ID
	private final List<Integer> SEVEN_PRODUCT_IDS = Arrays.asList(470, 358, 1305, 4, 2, 1178, 17);
	// 推薦熱點的產品 ID
	private final List<Integer> PD_PRODUCT_IDS = Arrays.asList(1281, 1282, 1283);
	
	// 本月主打 七筆資料
	public List<ProductEntity> getSevenProduct() {
		    return productRepository.findAllById(SEVEN_PRODUCT_IDS);
	}
	// 推薦熱點系列資料
	public List<ProductEntity> getpdProduct() {
		    return productRepository.findAllById(PD_PRODUCT_IDS);
	}
	
	/**
	 * 【新增】取得所有產品資料 (包含本月主打和推薦熱點)
	 * 這樣 Controller 只需要呼叫一次 DB 即可取得所有產品。
	 */
	public List<ProductEntity> getAllProductsData() {
		// 結合兩個 ID 列表並去重，然後一次性查詢資料庫
		List<Integer> allIds = Stream.concat(SEVEN_PRODUCT_IDS.stream(), PD_PRODUCT_IDS.stream())
                .distinct()
                .collect(Collectors.toList());
		
		return productRepository.findAllById(allIds);
	}
	
	public List<CategoryEntity> getCategory() {
		 List<Integer> ids = Arrays.asList(1, 2, 3, 4);
		    return categoryRepository.findAllById(ids);
	}
}
