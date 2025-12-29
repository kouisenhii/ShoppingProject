package com.tw.shopping.main.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.CategoryStatsDto;
import com.tw.shopping.main.dto.DashboardStatsDto;
import com.tw.shopping.main.dto.SalesTrendDto;
import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/stats")
@Tag(name = "後臺管理系統", description = "取得訂單、商品、會員等資料")
public class AdminDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @Operation(summary = "取得後臺管理系統數據", description = "取得總銷售金額、總訂單、總會員數")
    public DashboardStatsDto getDashboardStats() {
        // 1. 獲取數據
        Long totalSales = orderRepository.sumTotalSales(); // 只算已付款
        Long totalOrders = orderRepository.count();        // 總訂單數 (包含未付款)
        Long totalUsers = userRepository.count();          // 總會員數

        // 2. 計算平均客單價 (避免除以零)
        // 邏輯：總銷售額 / 總訂單數 (或者用已付款訂單數當分母會更精準，這邊先用總數簡單算)
        int avgOrderValue = 0;
        if (totalOrders > 0) {
            avgOrderValue = (int) (totalSales / totalOrders);
        }

        // 3. 回傳 DTO
        return new DashboardStatsDto(totalSales, totalOrders, totalUsers, avgOrderValue);
    }
    
 // 【新增】取得熱銷分類統計
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
 // 【修改】統計熱銷分類 (支援子分類匯總)
    @GetMapping("/categories")
    @Operation(summary = "取得所有分類", description = "抓資料庫裡的所有分類")
    public List<CategoryStatsDto> getCategoryStats() {
        // 1. 取得所有分類設定 (為了查名稱和父分類)
        List<CategoryEntity> allCats = categoryRepository.findAll();
        Map<Integer, CategoryEntity> catMap = allCats.stream()
            .collect(Collectors.toMap(CategoryEntity::getCategoryid, c -> c));

        // 2. 取得商品統計 (格式: [categoryId, count])
        List<Object[]> rawStats = productRepository.countProductsGroupByCategory();

        // 3. 匯總邏輯 (Map<ParentId, Count>)
        // 目標：只統計 ID 為 1, 2, 3, 4 的四大分類
        Map<Integer, Long> finalStats = new HashMap<>();
        // 初始化 1~4 為 0，確保即使沒商品也會回傳 0
        finalStats.put(1, 0L); finalStats.put(2, 0L); finalStats.put(3, 0L); finalStats.put(4, 0L);

        for (Object[] row : rawStats) {
            Integer catId = (Integer) row[0];
            Long count = (Long) row[1];
            
            CategoryEntity cat = catMap.get(catId);
            if (cat != null) {
                // 判斷邏輯：
                // 如果它是 1~4 本身 -> 直接加總
                // 如果它是子分類 (有 parentid) -> 加總給 parentid
                Integer targetId = null;
                
                if (finalStats.containsKey(catId)) {
                    targetId = catId;
                } else if (cat.getParentid() != null && finalStats.containsKey(cat.getParentid())) {
                    targetId = cat.getParentid();
                }

                if (targetId != null) {
                    finalStats.put(targetId, finalStats.get(targetId) + count);
                }
            }
        }
        
    
        // 4. 轉換成 DTO 回傳
        List<CategoryStatsDto> result = new ArrayList<>();
        finalStats.forEach((id, count) -> {
            String name = catMap.get(id).getCname();
            result.add(new CategoryStatsDto(name, count));
        });

        return result;
    }
    
    // 【新增】取得銷售趨勢圖表資料
    @GetMapping("/trend")
    @Operation(summary = "取得銷售趨勢", description = "對資料庫裡的訂單金額做處理")
    public List<SalesTrendDto> getSalesTrend() {
        List<Object[]> results = orderRepository.findSalesTrend();
        List<SalesTrendDto> trends = new ArrayList<>();
        
        // 日期格式化 (轉成 1/1, 1/5 這種短格式)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");

        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            Long total = (Long) row[1];
            
            trends.add(new SalesTrendDto(date.format(formatter), total));
        }
        
        return trends;
    }

}