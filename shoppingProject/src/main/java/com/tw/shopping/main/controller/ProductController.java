package com.tw.shopping.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.ProductDto;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "提供商品搜尋、詳細資訊等 API") // 1. 設定分類名稱
public class ProductController {

    @Autowired
    private ProductService productService;

    // 網址範例: /api/products/search?mainCategory=kitchen&minPrice=100&keyword=刀
    @GetMapping("/search")
    @Operation(summary = "搜尋商品", description = "支援關鍵字、分類、價格範圍篩選與分頁排序") // 2. 設定 API 說明
    public Page<ProductDto> search(
        // required = false 代表這些參數是可選的，沒傳就是 null
        // 大分類
        @RequestParam(required = false) String mainCategory,
        // 小分類
        @RequestParam(required = false) String subCategory,
        // 最大金額
        @RequestParam(required = false) Integer maxPrice,
        // 最小金額
        @RequestParam(required = false) Integer minPrice,
        // 搜尋關鍵字
        @RequestParam(required = false) String keyword,
        // 預設頁碼 : 從 0 開始
        @RequestParam(defaultValue = "0") Integer page,
        //  預設每頁顯示的商品數量
        @RequestParam(defaultValue = "12") Integer size,
        // 排序
        @RequestParam(defaultValue = "default") String sort
    ) {

        Page<ProductEntity> productPage = productService.searchProducts(mainCategory, subCategory, maxPrice, minPrice, keyword, page, size, sort);

        return productPage.map(this::convertToProductDto);

    }

    private ProductDto convertToProductDto(ProductEntity entity) {
        ProductDto dto = new ProductDto();
        
        dto.setProductid(entity.getProductid()); 
        dto.setPname(entity.getPname());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setColor(entity.getColor());
        dto.setProductimage(entity.getProductimage());
        dto.setStock(entity.getStock());
    
        return dto;
    }
}
