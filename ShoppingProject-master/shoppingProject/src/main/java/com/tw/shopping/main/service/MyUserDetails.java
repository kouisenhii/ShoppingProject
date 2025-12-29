package com.tw.shopping.main.service;

import java.io.Serializable; // ⭐ 引入這個
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

// ⭐ 【關鍵修正】實作 Serializable 介面
public class MyUserDetails extends User implements Serializable { 

    // ⭐ 【關鍵修正】新增序列化版本號
    private static final long serialVersionUID = 1L; 

    private final Long userId;

    /**
     * 建構子：接收 UserEntity 資訊，並傳遞給父類別 User 的建構子。
     * @param userId 帳號的 Long ID (用於業務邏輯)
     * @param username 帳號名稱 (用於 Spring Security 驗證，通常是 Email)
     * @param password 帳號密碼 (加密後的)
     * @param authorities 權限列表
     */
    public MyUserDetails(
            Long userId, 
            String username, 
            String password, 
            Collection<? extends GrantedAuthority> authorities) {
        
        // 呼叫父類別 User 的建構子
        super(username, password, authorities); 
        this.userId = userId;
    }

    /**
     * 專門提供給 SecurityUtility 獲取 Long ID
     */
    public Long getUserId() {
        return userId;
    }
}