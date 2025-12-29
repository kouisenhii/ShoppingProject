package com.tw.shopping.main.dto;

import lombok.Getter;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * GoogleLoginUser
 * --------------------------------------------------------------
 * 這個類別是我們自訂的 OAuth2User 實作，用來包裝從 Google 拿到的使用者資訊。
 *
 * 角色：
 *   - 實作 Spring Security 需要的 OAuth2User 介面
 *   - 提供程式方便呼叫的 getter：getProvider(), getProviderUserId(), getEmail() ...
 *   - 在 Authentication.principal 裡面實際存放的就是這個物件
 *
 * 優點：
 *   - 不直接綁定 JPA Entity（UserinfoEntity），避免 LazyInitialization / Session 問題
 *   - 純粹作為「登入階段」的資料載體 (DTO)
 */
@Getter
public class GoogleUserLoginDto implements OAuth2User {

    /** 第三方登入的平台名稱，例如 "GOOGLE" */
    private final String provider;

    /** 第三方平台提供的使用者唯一 ID，例如 Google 的 "sub" */
    private final String providerUserId;

    /** 使用者 email（若 Google 有回傳） */
    private final String email;

    /** 使用者名稱 */
    private final String name;

    /** 使用者頭像網址 */
    private final String picture;

    /** 原始 attributes，保留完整 Google 回傳資料，需要時可以直接取用 */
    private final Map<String, Object> attributes;
    
 // 1. 新增欄位
    private Long localUserId;

    // 2. 補上 Setter (重要！因為 Service 沒設值，我們要靠 Handler 後補)
    public void setLocalUserId(Long localUserId) {
        this.localUserId = localUserId;
    }

    /**
     * 建構子，將欄位初始化
     */
    public GoogleUserLoginDto(String provider,
                           String providerUserId,
                           String email,
                           String name,
                           String picture,
                           Map<String, Object> attributes) {

        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.attributes = attributes;
    }

    /**
     * 回傳 OAuth2User 介面要求的 attributes，
     * Spring Security 有時會用到這個 Map。
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 回傳這個使用者的權限（roles）。
     * 這裡簡單寫死為 ROLE_USER。
     * 若未來要做後台管理 / 權限分級，可以改成從 DB 讀取。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * OAuth2User 介面要求的 getName()
     *
     * 這個 "name" 是「Spring Security 用來代表此使用者的唯一名稱」，
     * 不一定要等於真實姓名，你可以選擇：
     *   - 顯示名稱（display name）
     *   - providerUserId
     *
     * 這裡的實作：
     *   - 若 name 有值 → 用 name
     *   - 否則回傳 providerUserId（確保一定有值）
     */
    @Override
    public String getName() {
        return (name != null && !name.isEmpty()) ? name : providerUserId;
    }
}
