
package com.tw.shopping.main.service;

import com.tw.shopping.main.dto.LocalUserLoginDto;
import com.tw.shopping.main.dto.SessionUserDto;
import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocalLoginService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 執行 Local 帳號密碼登入流程
     *
     * 登入流程包含：
     * 1. 依 Email 查詢帳號是否存在
     * 2. 驗證輸入的密碼是否正確（BCrypt）
     * 3. 確認 UserAuthProvider 是否已存在 LOCAL 記錄，若無則自動補上
     * 4. 建立 SessionUser 寫入 HttpSession（完成登入）
     *
     * @param login LocalUserLoginDto（email + password）
     * @param req   HttpServletRequest（用於取得 HttpSession）
     * @return      SessionUser（存入 session 的使用者資料）
     */
    public SessionUserDto authenticateLocalUser(LocalUserLoginDto login, HttpServletRequest req) {

        /**
         * Step 1 — 查詢 Email 是否存在
         *
         * 若查無使用者：
         *   → 直接丟出例外，避免洩露資訊（不回傳「密碼錯誤」）
         */
        Optional<UserEntity> opt = userRepository.findByEmail(login.getEmail());
        if (opt.isEmpty()) {
            throw new RuntimeException("查無此信箱");
        }

        UserEntity user = opt.get();

        /**
         * Step 2 — 驗證密碼
         *
         * 使用 Spring Security 密碼編碼器（BCrypt）
         * passwordEncoder.matches(raw, encoded)
         *
         * 若比對失敗：
         *   → 固定回傳「帳號或密碼錯誤」避免暴力猜測帳密
         */
        if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            throw new RuntimeException("帳號或密碼錯誤");
        }

        /**
         * Step 3 — 確保 Provider 表有 LOCAL provider
         *
         * 若使用者本來就使用 Google/Line 註冊，後來第一次改用 Local 登入，
         * 則 provider 表中可能尚未寫入 LOCAL 的 provider 記錄。
         *
         * 若未找到 LOCAL：
         *   → 自動新增 provider 資料，保持使用者登入來源的一致性
         */
        Optional<UserAuthProviderEntity> existProvider =
                providerRepository.findByProviderAndProviderUserid("LOCAL", login.getEmail());

        if (existProvider.isEmpty()) {
        		UserAuthProviderEntity provider = UserAuthProviderEntity.builder()
                    .user(user)
                    .provider("LOCAL")
                    .providerUserid(login.getEmail())   // LOCAL 的 providerUserId = email
                    .providerEmail(login.getEmail())
                    .providerName(user.getName())
                    .build();

            providerRepository.save(provider);
        }
        
        System.out.println("============== 權限偵錯開始 ==============");
        System.out.println("正在登入的使用者 ID: " + user.getUserid());
        
        if (user.getRoles() == null) {
            System.out.println("❌ user.getRoles() 是 NULL！");
        } else if (user.getRoles().isEmpty()) {
            System.out.println("⚠️ user.getRoles() 是空的 (Size = 0)！請檢查 userrole 資料表");
        } else {
            System.out.println("✅ 成功抓到角色！數量: " + user.getRoles().size());
            user.getRoles().forEach(r -> System.out.println("   - 角色名稱: " + r.getRoleName()));
        }
        System.out.println("========================================");
        
        
        /**
         * Step 4 — 建立 Session
         *
         * 若 session 不存在 → 自動建立新的 session（true）
         * 建立 SessionUser（輕量化，不保存敏感資料）
         *
         * session key 為 "USER"
         * 之後所有需要登入狀態的 API 都可透過 session.getAttribute("USER") 取得
         */
        HttpSession session = req.getSession(true);
        
        // 賴 新增的
        // 取得使用者的第一筆角色 (假設一個用戶只有一個主要角色)
        String roleName = "ROLE_MEMBER"; // 預設值
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleName = user.getRoles().iterator().next().getRoleName();
        }

        SessionUserDto sessionUser = SessionUserDto.builder()
                .userId(user.getUserid())
                .email(user.getEmail())
                .name(user.getName())
                .role(roleName) //  賴 新增的 儲存角色
                .provider("LOCAL")
                .build();

        session.setAttribute("USER", sessionUser);
        
     // 賴 新增的 12/6   
     //  [新增程式碼開始]：載入資料庫角色並通知 Spring Security 
        try {
            // 1. 準備權限清單 (如果沒角色就是空清單，而不是跳過)
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            if (user.getRoles() != null) {
                authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                        .collect(Collectors.toList());
            }
            
          //建立 MyUserDetails 實例作為 Principal
            MyUserDetails myUserDetails = new MyUserDetails(
                user.getUserid(), 
                user.getEmail(), 
                "", // 密碼不需要，但 MyUserDetails 構造子需要
                authorities
            );
            
            // 2. 建立認證 Token (即使 authorities 是空的也沒關係，代表已登入但無權限)
            UsernamePasswordAuthenticationToken authentication = 
              new UsernamePasswordAuthenticationToken(myUserDetails, null, authorities);
          
            // 3. 設定 Context
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 4. 強制寫入 Session
            session.setAttribute("SPRING_SECURITY_CONTEXT", context); 
            
        } catch (Exception e) {
            System.err.println("登入權限設定異常: " + e.getMessage());
        }
        //  [新增程式碼結束] 
        
        

        /**
         * 回傳 SessionUser（給 Controller 使用）
         */
        return sessionUser;
    }
}
