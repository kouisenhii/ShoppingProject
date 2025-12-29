package com.tw.shopping.main.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.dto.CategoryWithCountDto;
import com.tw.shopping.main.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {
    // 用code 去找大分類
    CategoryEntity findByCode(String code);

    // 用 parentId 找子分類列表 (找出所有屬於該大分類的小項目，單純找實體)
    List<CategoryEntity> findByParentid(Integer parentid);

    // 找出子分類並計算商品數量
    // 原理：選出分類 (c)，並 LEFT JOIN 商品 (p)，然後 GROUP BY 分類來計算 COUNT(p)
    // new com.tw.shopping.main.dto.CategoryWithCountDTO(c.code, c.cname, COUNT(p) 這段的意思是:
    // 我只查 code、cname、COUNT 查完後放進我 new 出來的這個物件 (List<CategoryWithCountDto>) 裡
    @Query("SELECT new com.tw.shopping.main.dto.CategoryWithCountDto(c.code, c.cname, COUNT(p)) " +
            "FROM CategoryEntity c " +
            "LEFT JOIN c.products p " +
            "WHERE c.parentid = :parentid " +
            "GROUP BY c.categoryid, c.code, c.cname")
    List<CategoryWithCountDto> findSubCategoriesWithCount(@Param("parentid") Integer parentid);
}
