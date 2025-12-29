package com.tw.shopping.main.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.CategoryDto;
import com.tw.shopping.main.dto.HomeDataResponseDto;
import com.tw.shopping.main.dto.ProductDto;
import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.service.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
// 將基底路徑改為統一的端點，例如 /home (您可以自行決定名稱)
@RequestMapping("/home")
@Tag(name = "首頁", description = "取得首頁所有資料")
public class Controller {

    @Autowired
    private HomeService service;

    /**
     * 【統一 API】取得首頁所有資料 (本月主打、推薦熱點、產品系列)
     */
    @GetMapping
    @Operation(summary = "首頁資料", description = "至資料庫抓取資料")
    public HomeDataResponseDto getHomeDate() {

        // 1. 從 Service 取得所有 Entity 資料
        List<ProductEntity> mainProductEntities = service.getSevenProduct();
        List<ProductEntity> pdProductEntities = service.getpdProduct();
        List<CategoryEntity> categoryEntities = service.getCategory();

        // 2. 將 Entity 轉換為 DTO
        List<ProductDto> mainProductDtos = mainProductEntities.stream()
                .map(this::convertToProductDto)
                .collect(Collectors.toList());

        List<ProductDto> pdProductDtos = pdProductEntities.stream()
                .map(this::convertToProductDto)
                .collect(Collectors.toList());

        List<CategoryDto> categoryDtos = categoryEntities.stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());

        // 3. 封裝結果並返回
        HomeDataResponseDto response = new HomeDataResponseDto();
        response.setMainProducts(mainProductDtos);
        response.setFeaturedProducts(pdProductDtos);
        response.setCategories(categoryDtos);

        return response;
    }

    /**
     * Product Entity 轉 Product DTO
     */
    private ProductDto convertToProductDto(ProductEntity entity) {
        ProductDto dto = new ProductDto();

        dto.setProductid(entity.getProductid());
        dto.setPname(entity.getPname());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setColor(entity.getColor());
        dto.setProductimage(entity.getProductimage());

        return dto;
    }

    /**
     * Category Entity 轉 Category DTO
     */
    private CategoryDto convertToCategoryDto(CategoryEntity entity) {
        CategoryDto dto = new CategoryDto();

        dto.setCategoryid(entity.getCategoryid());
        dto.setCname(entity.getCname());
        dto.setCategoryimage(entity.getCategoryimage());
        dto.setCode(entity.getCode());

        return dto;
    }
}