package com.tw.shopping.main.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.CategoryDto;
import com.tw.shopping.main.dto.CategoryWithCountDto;
import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.repository.CategoryRepository;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Cacheable(value = "subCategories", key = "#mainCategoryCode")
    public List<CategoryWithCountDto> getSubCategoriesByMainCategory(String mainCategoryCode){
        // 先找出大分類的實體，為了拿到它的 ID
        CategoryEntity mainCategory = categoryRepository.findByCode(mainCategoryCode);

        // 如果找不到 ID 回傳空字串
        if (mainCategory == null) {
            return Collections.emptyList();
        }

        // 傳入大分類的 ID (parentid)，取得帶有數量的子分類列表
        return categoryRepository.findSubCategoriesWithCount(mainCategory.getCategoryid());
    }

    // 取得所有 parentId 是 null (大分類)的
    @Cacheable(value = "mainCategories")
    public List<CategoryDto> getMainCategories() {
        List<CategoryEntity> entities = categoryRepository.findByParentid(null);
        
        // 把 Entity 轉換成 DTO
        return entities.stream().map(e -> new CategoryDto(
            e.getCategoryid(),
            e.getCname(),
            e.getCode(),
            e.getCategoryimage(),
            e.getParentid()
        )).collect(Collectors.toList());
    }
}
