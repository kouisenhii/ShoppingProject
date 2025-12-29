package com.tw.shopping.main.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer>, JpaSpecificationExecutor<ProductEntity>{
	
	//  // 賴 新增的 11/30 
	// 【新增】後台搜尋專用查詢
    // 邏輯：
    // 1. 如果 keyword 是 null，就忽略名稱搜尋
    // 2. 如果 categoryId 是 null，就忽略分類過濾
	@Query("SELECT p FROM ProductEntity p WHERE " +
	           "(:keyword IS NULL OR p.pname LIKE %:keyword%) AND " +
	           "(:categoryId IS NULL OR p.category.categoryid = :categoryId)")
	    Page<ProductEntity> searchProducts(
	            @Param("keyword") String keyword, 
	            @Param("categoryId") Integer categoryId, 
	            Pageable pageable);
	// 哈哈還是我加的啦
	// 【新增】統計前四大分類的商品數量 (ID 1, 2, 3, 4)
    // 注意：這裡假設您的 ProductEntity 有關聯 CategoryEntity (private CategoryEntity category)
	// 【修改】查詢每個分類ID有多少商品 (不限前四名，全部都查出來)
    @Query("SELECT p.category.categoryid, COUNT(p) FROM ProductEntity p GROUP BY p.category.categoryid")
    List<Object[]> countProductsGroupByCategory();

	// ========================================================================
    // 🔥【重點修改】使用 SQL 原子更新來扣減庫存
    // 不需要 @Lock，直接利用 UPDATE 語句的原子性。
    // 回傳值 int 代表「實際上更新了幾筆資料」。
    // 邏輯：只有當 stock >= quantity 時才執行扣減，否則 WHERE 條件不成立，回傳 0。
    // ========================================================================
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.productid = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
