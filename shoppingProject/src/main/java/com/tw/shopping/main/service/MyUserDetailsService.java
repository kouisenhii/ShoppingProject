package com.tw.shopping.main.service;


import org.springframework.transaction.annotation.Transactional;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Transactional(readOnly = true)
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userInfoRepo;

    public MyUserDetailsService(UserRepository userInfoRepo) {
        this.userInfoRepo = userInfoRepo; 
    }

    // 載入使用者
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
     
        UserEntity user = userInfoRepo.findByEmail(email) 
                .orElseThrow(() -> {
                    return new ResourceNotFoundException("找不到使用者!" );
                });
            
        // 🌟 關鍵修正：不再將 ID 塞入 username 欄位
        // 創建角色列表 (這裡使用硬編碼 "USER"，如果資料庫有角色欄位，應使用 user.getRole())
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
   
        // 🌟 關鍵修正：返回自定義的 MyUserDetails 實例
        return new MyUserDetails(
            user.getUserid(),              // 1. 將 Long ID 傳入 MyUserDetails 專屬欄位
            user.getEmail(),               // 2. 將 Email 設為標準的 username (用於登入驗證)
            user.getPassword(),            // 3. 加密後的密碼
            Collections.singleton(authority) // 4. 權限
        );
    }
}