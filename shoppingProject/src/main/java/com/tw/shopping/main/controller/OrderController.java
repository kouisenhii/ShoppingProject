package com.tw.shopping.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.CheckoutRequestDto;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "下訂單", description = "將購物車轉為訂單")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 注意：這裡不需要 EcpayService，因為這個 API 只負責 "建立訂單"
    // 綠界表單的產生，是前端拿到 ID 後，下一步去呼叫 EcpayController 處理的

    @PostMapping("/checkout")
    @Operation(summary = "下訂單", description = "建立訂單至資料庫")
    public ResponseEntity<OrderEntity> checkout(@RequestBody CheckoutRequestDto request) {
        // 這是我臣又貝改的拉 嘿嘿
        // 我完全移掉原本的try-catch，讓例外直接往上拋
        // 因為我們已經有 GlobalExceptionHandler 來處理例外了
        // 如果 createOrder 成功就回傳200 OK 和訂單資料
        // 如果 createOrder 拋出 StockNotEnoughException (庫存不足)
        // 最後被我們寫好的 GlobalExceptionHandler 攔截，
        // 並回傳格式統一的 JSON (ErrorResponseDto) 給前端。


        // 1. 將購物車轉為訂單
        OrderEntity order = orderService.createOrder(request);

        // 2. 【關鍵修正】直接回傳 OrderEntity 物件 (JSON)
        // 這樣前端 cart.js 才能成功讀取到 orderResponse.orderId
        return ResponseEntity.ok(order);
    }
}