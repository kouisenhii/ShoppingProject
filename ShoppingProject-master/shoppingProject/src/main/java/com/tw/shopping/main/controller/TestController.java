package com.tw.shopping.main.controller;

import com.tw.shopping.main.util.SecurityUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final SecurityUtility securityUtility;

    // 瀏覽器輸入: http://localhost:8080/test/check-id
    @GetMapping("/check-id")
    public String checkId() {
        return securityUtility.getCurrentUserId()
                .map(id -> "抓到了！你的資料庫 User ID 是: " + id)
                .orElse("抓不到 ID (可能是沒登入，或 Handler 沒生效)");
    }
}