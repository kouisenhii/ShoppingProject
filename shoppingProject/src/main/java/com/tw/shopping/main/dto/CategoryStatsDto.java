package com.tw.shopping.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryStatsDto {
    private String categoryName; // cname
    private Long count;          // 商品數量
}