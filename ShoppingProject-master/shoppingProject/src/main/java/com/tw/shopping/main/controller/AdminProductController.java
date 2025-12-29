package com.tw.shopping.main.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.repository.ProductRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "後臺管理系統", description = "取得商品")
public class AdminProductController {

    @Autowired
    private ProductRepository productRepository;

    // 1. 取得商品列表 (支援搜尋、過濾、排序)
    @GetMapping
    @Operation(summary = "取得商品資料", description = "抓取所有商品")
    public Page<ProductEntity> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "NEWEST") String sortType,
            @RequestParam(defaultValue = "0") int page, // 新增：頁碼 (從0開始)
            @RequestParam(defaultValue = "10") int size // 新增：每頁幾筆
    ) {

        // 1. 處理排序 (記得要用 createdAt)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        switch (sortType) {
            case "PRICE_ASC": sort = Sort.by(Sort.Direction.ASC, "price"); break;
            case "PRICE_DESC": sort = Sort.by(Sort.Direction.DESC, "price"); break;
            case "STOCK_ASC": sort = Sort.by(Sort.Direction.ASC, "stock"); break;
            case "STOCK_DESC": sort = Sort.by(Sort.Direction.DESC, "stock"); break;
            case "NEWEST": default: sort = Sort.by(Sort.Direction.DESC, "createdAt"); break;
        }

        // 2. 建立分頁請求 (包含排序)
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. 呼叫 Repository
        return productRepository.searchProducts(keyword, categoryId, pageable);
    }

    // 2. 新增商品
    @PostMapping
    @Operation(summary = "新增商品", description = "新增商品至資料庫")
    public ProductEntity createProduct(@RequestBody ProductEntity product) {
        // 實際專案中，這裡通常會處理圖片上傳 (MultipartFile)，
        // 目前先假設前端傳來的是圖片 URL 字串
        return productRepository.save(product);
    }

    // 3. 刪除商品
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除商品", description = "根據id刪除商品")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // 4. (選用) 更新商品 - 如果您之後要實作編輯功能
    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "根據id更新商品")
    public ResponseEntity<ProductEntity> updateProduct(@PathVariable Integer id, @RequestBody ProductEntity productDetails) {
         return productRepository.findById(id).map(product -> {
            product.setPname(productDetails.getPname());
            product.setPrice(productDetails.getPrice());
            product.setStock(productDetails.getStock());
            product.setCategory(productDetails.getCategory());
            product.setColor(productDetails.getColor());
            product.setSpecification(productDetails.getSpecification());
            product.setDescription(productDetails.getDescription());
            // ...其他欄位
            return ResponseEntity.ok(productRepository.save(product));
        }).orElse(ResponseEntity.notFound().build());
    }
}