package com.tw.shopping.main.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * AuthResponse
 *
 * 此 DTO 用於「後端登入成功後回傳給前端」的使用者資料。
 *
 * 使用場景：
 *  - Local 帳密登入成功時回傳給前端
 *  - 第三方登入（Google / LINE / Facebook）成功時回傳
 *  - 前端需要顯示登入後的使用者資訊（navbar / 個人頭像區塊）
 *
 * 設計目的：
 *  - 統一後端所有登入方式的回傳格式
 *  - 僅包含前端需要的欄位，避免洩漏敏感資料（例如密碼）
 *  - 前端可不依賴 Session，直接使用這份資訊更新 UI
 *
 * 欄位說明：
 *  - userId   → 使用者唯一編號
 *  - name     → 顯示名稱
 *  - email    → 註冊 Email
 *  - provider → 登入方式（LOCAL / GOOGLE / LINE / FACEBOOK）
 *  - avatar   → 頭像（Base64 或 URL）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private Long userId;
    private String name;
    private String email;
    private String provider;
    private String avatar;
    private String role; // 賴 新增的 12/6
}
