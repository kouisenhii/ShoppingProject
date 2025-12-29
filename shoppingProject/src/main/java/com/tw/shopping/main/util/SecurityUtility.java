package com.tw.shopping.main.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.tw.shopping.main.dto.GoogleUserLoginDto;
import com.tw.shopping.main.dto.LineUserLoginDto;
import com.tw.shopping.main.service.MyUserDetails;

@Component
public class SecurityUtility {
    
    // 引入日誌工具
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtility.class);

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 1. 獲取當前登入用戶的 ID
     */
    public Optional<Long> getCurrentUserId() {
        return getAuthentication()
                // 檢查是否已認證
                .filter(Authentication::isAuthenticated) 
                .map(Authentication::getPrincipal)
                // 過濾掉匿名用戶
                .filter(principal -> principal != null && !principal.equals("anonymousUser"))
                
                // 🌟 關鍵修正：將 Principal 轉換為 MyUserDetails 實例並提取 ID
                .flatMap(principal -> {
                    // Local
                    if (principal instanceof MyUserDetails myUserDetails) {
                        return Optional.of(myUserDetails.getUserId());
                    } 
                    // Google (ID 是剛剛在 SuccessHandler 補填進去的)
                    else if (principal instanceof GoogleUserLoginDto googleUser) {
                        return Optional.ofNullable(googleUser.getLocalUserId());
                    }
                    // Line
                    else if (principal instanceof LineUserLoginDto lineUser) {
                        return Optional.ofNullable(lineUser.getLocalUserId());
                    }
                    // ...
                    return Optional.empty();
                });       
    }
    
//	檢查使用者是否已登入
	public Long getCurrentUserIdOrThrow() {
        return getCurrentUserId().orElseThrow(
            () -> new InsufficientAuthenticationException("請先登入"));
    }
    /**
     * 2. 檢查當前用戶是否已認證 (已登入，排除匿名用戶)
     */
    public boolean isAuthenticated() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .map(p -> !p.equals("anonymousUser"))
                .orElse(false);
    }

    /**
     * 3. 獲取當前用戶的 Principal 對象 (排除匿名用戶)
     */
    public Optional<Object> getCurrentUserPrincipal() {
         return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(p -> !p.equals("anonymousUser"));
    }

    /**
     * 4. 獲取當前用戶的所有權限/角色列表
     */
    public Collection<String> getAuthorities() {
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .orElse(Collections.emptySet())
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}




