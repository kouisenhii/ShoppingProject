package com.tw.shopping.main.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.repository.CategoryRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "後臺管理系統", description = "取得分類")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // 取得所有分類
    @GetMapping
    @Operation(summary = "取得所有分類", description = "抓資料庫裡的所有分類")
    public List<CategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }
}
