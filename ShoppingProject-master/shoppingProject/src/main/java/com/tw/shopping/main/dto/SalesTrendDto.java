package com.tw.shopping.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalesTrendDto {
    private String date; // 日期 (格式: MM/dd)
    private Long total;  // 當日銷售總額
}