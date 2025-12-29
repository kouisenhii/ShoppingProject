package com.tw.shopping.main.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.ProductRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class ProductService {
    // 定義 Logger (用來印出系統信息)
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    // 用來查大分類的 ID
    @Autowired
    private CategoryRepository categoryRepository;

    public Page<ProductEntity> searchProducts(String mainCategoryCode, String subCategoryCode, Integer maxPrice, Integer minPrice, String keyword, Integer page, Integer size, String sort){

        // 設定那些排序
        Sort sortOption = switch (sort) {
            case "latest" -> Sort.by("createdAt").descending();
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "ratingAsc" -> Sort.by("rating").ascending();
            case "ratingDesc" -> Sort.by("rating").descending();
            default -> Sort.unsorted();
        };

        // 建立 Pageable 物件
        // PageRequest.of(頁碼, 每頁大小)
        // Spring Data JPA 的頁碼從 0 開始 每頁 12 筆資料
        Pageable pageable = PageRequest.of(page, size, sortOption);

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 當使用者選了小分類的篩選
            if (StringUtils.hasText(subCategoryCode)) {
                // 用 SQL 查詢把品項跟點選的Code相同的給拉出來 因為 code 在 category的表所以要先get到category在 get code
                predicates.add(cb.equal(root.get("category").get("code"), subCategoryCode));
            }

            // 使用者選擇大分類還沒選小分類
            else if (StringUtils.hasText(mainCategoryCode)) {
                // 用大分類的 code 找到他的 ID
                CategoryEntity mainCat = categoryRepository.findByCode(mainCategoryCode);

                // 找出所有 ID 等於所選的大分類的商品
                if (mainCat != null) {
                    predicates.add(cb.equal(root.get("category").get("parentid"), mainCat.getCategoryid()));
                }
            }

            // 價格範圍
            // 找出所有大於最小金額的商品
            if (minPrice != null){
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            // 找出所以小於最大金額的商品
            if (maxPrice != null){
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Keyword 搜尋
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate nameLike = cb.like(root.get("pname"), likePattern);
                Predicate descLike = cb.like(root.get("description"), likePattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ProductEntity> result = productRepository.findAll(spec, pageable);

        // 商業邏輯監控：紀錄搜尋無結果的關鍵字
        // 條件：使用者有關鍵字搜尋 + 結果數量為 0
        if (StringUtils.hasText(keyword) && result.getTotalElements() == 0) {
            // 使用 WARN 層級，方便在大量 Log 中一眼看到
            logger.warn("🛑 [潛在商機流失] 使用者搜尋關鍵字: '{}'，但系統中無此商品。", keyword);
        }

        return result;
    }
}
