package com.tw.shopping.main.dto; // 或是放在 service/security 套件

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private OAuth2User oauth2User; // 原本 Google/Line 回傳的 User
    private Long userId;           // 我們資料庫的 PK

    public CustomOAuth2User(OAuth2User oauth2User, Long userId) {
        this.oauth2User = oauth2User;
        this.userId = userId;
    }

    // 重點：提供方法取得 userId
    public Long getUserId() {
        return userId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getAttribute("name"); // 或其他顯示名稱的欄位
    }
    
    // 如果需要 email
    public String getEmail() {
        return oauth2User.getAttribute("email"); 
    }
}