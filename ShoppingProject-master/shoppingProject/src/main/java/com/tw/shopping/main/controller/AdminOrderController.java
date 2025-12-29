package com.tw.shopping.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.repository.OrderRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "後臺管理系統", description = "取得訂單細項")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    @Operation(summary = "取得訂單", description = "抓取所有訂單")
    public Page<OrderEntity> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, // 前端傳來的是數字 (0, 1, 2...)
            @RequestParam(required = false, defaultValue = "DATE_DESC") String sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 1. 處理排序
        Sort sort = Sort.by(Sort.Direction.DESC, "orderDate");
        switch (sortType) {
            case "DATE_ASC": sort = Sort.by(Sort.Direction.ASC, "orderDate"); break;
            case "AMOUNT_DESC": sort = Sort.by(Sort.Direction.DESC, "totalAmount"); break;
            case "AMOUNT_ASC": sort = Sort.by(Sort.Direction.ASC, "totalAmount"); break;
            case "DATE_DESC": default: sort = Sort.by(Sort.Direction.DESC, "orderDate"); break;
        }

        // 2. 處理狀態 (Integer -> Enum)
        OrderStatus orderStatus = null;
        if (status != null) {
            // 假設 Enum 順序為 0:PENDING, 1:SHIPPING... 依此類推
            // 務必確保您的 OrderStatus 定義順序與前端 value 一致
            if (status >= 0 && status < OrderStatus.values().length) {
                orderStatus = OrderStatus.values()[status];
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return orderRepository.searchOrders(keyword, orderStatus, pageable);
    }
}