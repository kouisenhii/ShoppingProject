package com.tw.shopping.main.service;


import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionManagementService {

    private final SessionRegistry sessionRegistry;

    // 注入 SessionRegistry
    public SessionManagementService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 強制使特定使用者所有活動中的 Session 失效 (登出)。
     * @param userId 當前登入使用者的 Long ID
     */
    public void expireAllSessionsForUser(Long userId) {
        
        // 取得所有 Principal (已登入的使用者對象)
        List<Object> principals = sessionRegistry.getAllPrincipals();

        // 將 Long 轉為 String，以便後續比對，假設您的 UserDetails.getUsername() 返回的是 ID 的 String 形式
        String targetPrincipalId = String.valueOf(userId); 

        for (Object principal : principals) {
            
            // 確保 Principal 是 UserDetails 實作
            if (principal instanceof UserDetails) {
                 UserDetails userDetails = (UserDetails) principal;
                 
                 // 這裡需要根據您的 UserDetails 實作來取得使用者 ID。
                 // 💡 假設：您的 UserDetails 實作中，getUsername() 返回的是使用者的 Long ID (String 形式)。
                 if (userDetails.getUsername().equals(targetPrincipalId)) {
                     
                    // 取得該 Principal 相關的所有活動 Session 資訊 (第二個參數為 'includeExpiredSessions' = false)
                    sessionRegistry.getAllSessions(principal, false)
                        .forEach(sessionInformation -> {
                            // 呼叫 expireNow() 立即標記 Session 為過期，下次請求時將被要求重新登入
                            sessionInformation.expireNow();
                        });
                    // 因為 Principal 是唯一的，找到後可以直接退出循環
                    return; 
                 }
            }
        }
    }
}