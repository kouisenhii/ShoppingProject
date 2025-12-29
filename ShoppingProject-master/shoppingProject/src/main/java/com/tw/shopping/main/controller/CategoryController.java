package com.tw.shopping.main.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.CategoryDto;
import com.tw.shopping.main.dto.CategoryWithCountDto;
import com.tw.shopping.main.service.CategoryService;


@RestController
@RequestMapping("/api/categories")
// @CrossOrigin(origins = "*")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    // 取得所有大分類 (nav上面的那排)
    @GetMapping("/main")
    public List<CategoryDto> getMainCateGories() {
        return categoryService.getMainCategories();
    }
    
    // 使用者點選大分類之後取得其底下的小分類
    @GetMapping("/main/{code}/sub")
    public ResponseEntity<List<CategoryWithCountDto>> getSubCategories(@PathVariable String code) {
        List<CategoryWithCountDto> list = categoryService.getSubCategoriesByMainCategory(code);
        return ResponseEntity.ok(list);
    }
}
