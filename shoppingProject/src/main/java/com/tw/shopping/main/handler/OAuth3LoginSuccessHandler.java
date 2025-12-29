package com.tw.shopping.main.handler;

import com.tw.shopping.main.dto.GoogleUserLoginDto;
import com.tw.shopping.main.dto.LineUserLoginDto;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler; // 改用介面
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth3LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        
        // 1. 取得 Principal
        Object principal = authentication.getPrincipal();
        String email = null;

        // 2. 判斷型別取 Email
        if (principal instanceof GoogleUserLoginDto googleUser) {
            email = googleUser.getEmail();
        } else if (principal instanceof LineUserLoginDto lineUser) {
            email = lineUser.getEmail();
        }

        // 3. 補填 ID 邏輯
        if (email != null) {
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                Long dbId = userOpt.get().getUserid();
                
                if (principal instanceof GoogleUserLoginDto googleUser) {
                    googleUser.setLocalUserId(dbId);
                    System.out.println("✅ [OAuth3] 已將 Google User ID (" + dbId + ") 注入 Session DTO");
                } else if (principal instanceof LineUserLoginDto lineUser) {
                    lineUser.setLocalUserId(dbId);
                    System.out.println("✅ [OAuth3] 已將 LINE User ID (" + dbId + ") 注入 Session DTO");
                }
            } else {
                System.out.println("⚠️ [OAuth3] 查無此 Email (" + email + ")，無法注入 ID (可能是新用戶)");
            }
        }
        
        // ❌ 重點修改：這裡絕對不要呼叫 super.onAuthenticationSuccess 或 response.sendRedirect
        // 因為我們要讓後面的 OAuth2LoginSuccessHandler 去決定要跳轉到哪裡
    }
}