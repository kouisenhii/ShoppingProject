package com.tw.shopping.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {
    private Long totalSales;      // 總銷售額
    private Long totalOrders;     // 總訂單數
    private Long totalUsers;      // 會員總數
    private Integer avgOrderValue;// 平均客單價
}