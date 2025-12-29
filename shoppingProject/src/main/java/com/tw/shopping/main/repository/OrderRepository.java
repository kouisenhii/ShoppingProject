package com.tw.shopping.main.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer>{
	Optional<OrderEntity> findById(Integer orderId);
	
	
	// 賴 新增的 11/29
	Optional<OrderEntity> findByEcpaytradeno(String ecpaytradeno);
	// 【新增】後台訂單搜尋與分頁
    // 邏輯：
    // 1. keyword 為 null 時忽略，否則將 orderId 轉字串比對
    // 2. status 為 null 時忽略，否則比對 Enum
    @Query("SELECT o FROM OrderEntity o WHERE " +
           "(:keyword IS NULL OR CAST(o.orderId AS string) LIKE %:keyword%) AND " +
           "(:status IS NULL OR o.orderStatus = :status)")
    Page<OrderEntity> searchOrders(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            org.springframework.data.domain.Pageable pageable);
    // 哈哈 是我新增的啦
    // 【新增】計算總銷售額 (只計算已付款 PAID 的訂單)
    // COALESCE(SUM(...), 0) 的意思是：如果沒有任何訂單，SUM 會是 NULL，這時回傳 0
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.paymentstatus = 'PAID'")
    Long sumTotalSales();
    // 哈哈全都是我啦
    // 【新增】銷售趨勢查詢
    // 邏輯：只查已付款 (PAID)，依照日期 (LocalDate) 分組，並算出總金額
    // 注意：CAST(o.orderDate AS LocalDate) 是將 timestamp 轉為單純日期
    @Query("SELECT CAST(o.orderDate AS LocalDate), SUM(o.totalAmount) " +
           "FROM OrderEntity o " +
           "WHERE o.paymentstatus = 'PAID' " +
           "GROUP BY CAST(o.orderDate AS LocalDate) " +
           "ORDER BY CAST(o.orderDate AS LocalDate) ASC")
    List<Object[]> findSalesTrend();

    List<OrderEntity> findByUserid_Userid(Long userId);
    Optional<OrderEntity> findByOrderIdAndUserid_Userid(Integer orderId, Long userId);
	
}
