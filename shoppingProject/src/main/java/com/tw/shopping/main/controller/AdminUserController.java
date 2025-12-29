package com.tw.shopping.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "後臺管理系統", description = "取得會員資訊")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @Operation(summary = "取得會員資料", description = "抓取所有會員資料")
    public Page<UserEntity> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String verified, // 前端傳 "VERIFIED", "UNVERIFIED", "ALL"
            @RequestParam(required = false, defaultValue = "JOIN_DATE_DESC") String sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 1. 處理排序
        // 假設您的 UserEntity 建立時間欄位叫 "createdat" (請對照您的 Entity 修改)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); 
        
        switch (sortType) {
            case "JOIN_DATE_ASC": sort = Sort.by(Sort.Direction.ASC, "createdAt"); break;
            case "NAME_ASC": sort = Sort.by(Sort.Direction.ASC, "name"); break;
            case "NAME_DESC": sort = Sort.by(Sort.Direction.DESC, "name"); break;
            case "JOIN_DATE_DESC": default: sort = Sort.by(Sort.Direction.DESC, "createdAt"); break;
        }

        // 2. 處理驗證狀態 (String -> Boolean)
        Boolean isVerified = null;
        if ("VERIFIED".equals(verified)) {
            isVerified = true;
        } else if ("UNVERIFIED".equals(verified)) {
            isVerified = false;
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.searchUsers(keyword, isVerified, pageable);
    }
    
    // 刪除會員 (慎用)
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除會員", description = "根據id刪除會員")
    public void deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
    }
}