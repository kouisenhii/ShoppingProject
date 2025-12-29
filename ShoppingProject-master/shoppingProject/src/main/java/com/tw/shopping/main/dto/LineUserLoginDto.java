package com.tw.shopping.main.dto;

import lombok.Getter;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * LineUserLoginDto
 * --------------------------------------------------------------
 * 用來包裝從 LINE 拿到的使用者資訊。
 * 結構與 GoogleUserLoginDto 幾乎一樣，確保風格統一。
 */
@Getter
public class LineUserLoginDto implements OAuth2User {

    private final String provider;
    private final String providerUserId; // 對應 LINE 的 userId (sub)
    private final String email;
    private final String name;
    private final String picture;
    private final Map<String, Object> attributes;
    
 // 1. 新增欄位
    private Long localUserId;

    // 2. 補上 Setter (重要！因為 Service 沒設值，我們要靠 Handler 後補)
    public void setLocalUserId(Long localUserId) {
        this.localUserId = localUserId;
    }

    public LineUserLoginDto(String provider,
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

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return (name != null && !name.isEmpty()) ? name : providerUserId;
    }
}