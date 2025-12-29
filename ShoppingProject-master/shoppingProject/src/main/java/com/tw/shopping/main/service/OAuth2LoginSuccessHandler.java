package com.tw.shopping.main.service;

import com.tw.shopping.main.dto.GoogleUserLoginDto;
import com.tw.shopping.main.dto.LineUserLoginDto;
import com.tw.shopping.main.dto.SessionUserDto;
import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2LoginSuccessHandler
 * --------------------------------------------------------------
 * 通用的 OAuth2 登入成功處理器。
 * 修正版：兼容標準 OIDC 流程 (LINE) 與 自定義 OAuth2 流程 (Google)。
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException {

        // 1. 取得 Provider 名稱 (google 或 line)
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId(); 

        Object principal = authentication.getPrincipal();

        // 宣告變數來接收資料
        String providerUserId = null;
        String email = null;
        String name = null;
        String picture = null;

        // 2. 根據回傳的物件類型，提取資料
        // -----------------------------------------------------------------------
        // Case A: OIDC User (LINE 修正後會走這裡，或者 Google 開啟 OIDC 也會走這裡)
        if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            // OIDC 標準欄位
            providerUserId = oidcUser.getSubject();      // sub
            email = oidcUser.getEmail();                 // email
            name = oidcUser.getFullName();               // name
            picture = oidcUser.getPicture();             // picture
            
            // 如果 OidcUser 沒抓到，嘗試從 Attributes 補抓 (LINE 有時欄位名稱不同)
            Map<String, Object> attributes = oidcUser.getAttributes();
            if (picture == null) picture = (String) attributes.get("picture");
            if (name == null) name = (String) attributes.get("name");

        } 
        // Case B: Google DTO (維持您原本的邏輯)
        else if (principal instanceof GoogleUserLoginDto) {
            GoogleUserLoginDto googleUser = (GoogleUserLoginDto) principal;
            providerUserId = googleUser.getProviderUserId();
            email = googleUser.getEmail();
            name = googleUser.getName();
            picture = googleUser.getPicture();
            // 如果 DTO 裡面的 provider 是 null，就用 token 裡的
            if (googleUser.getProvider() != null) {
                provider = googleUser.getProvider();
            }
        } 
        // Case C: Line DTO (舊邏輯備用，理論上改了 Config 後不會進來這裡)
        else if (principal instanceof LineUserLoginDto) {
            LineUserLoginDto lineUser = (LineUserLoginDto) principal;
            providerUserId = lineUser.getProviderUserId();
            email = lineUser.getEmail();
            name = lineUser.getName();
            picture = lineUser.getPicture();
        } 
        // Case D: 通用 Fallback (避免未知的型別導致 NullPointerException)
        else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            Map<String, Object> attributes = oauthUser.getAttributes();
            // 嘗試通用欄位
            providerUserId = (String) attributes.getOrDefault("sub", attributes.get("id"));
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            picture = (String) attributes.get("picture");
        }

        // 防呆：如果真的什麼都沒抓到 (不太可能發生)
        if (providerUserId == null) {
            response.sendRedirect("/html/login.html?error=unknown_user_type");
            return;
        }

        // --- 以下邏輯與您原本的資料庫處理完全相同 ---

        // 3. 檢查是否已綁定 Provider
        Optional<UserAuthProviderEntity> providerOpt =
                providerRepository.findByProviderAndProviderUserid(provider, providerUserId);
        
        if (providerOpt.isPresent()) {
            UserEntity user = providerOpt.get().getUser();
            setSessionUser(request, user, provider, picture);
            response.sendRedirect("/index.html");
            return;
        }

        // 4. 檢查 Email 是否已存在 (進行綁定)
        Optional<UserEntity> userOpt = (email != null) ? userRepository.findByEmail(email) : Optional.empty();
        
        if (userOpt.isPresent()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("PENDING_BIND_PROVIDER", provider);
            session.setAttribute("PENDING_BIND_EMAIL", email);
            session.setAttribute("PENDING_BIND_NAME", name);
            session.setAttribute("PENDING_BIND_PICTURE", picture);
            session.setAttribute("PENDING_BIND_PROVIDER_USERID", providerUserId);
            response.sendRedirect("/html/bind_account.html");
            return;
        }

        // 5. 全新註冊
        UserEntity newUser = UserEntity.builder()
                .email(email)
                .name(name)
                .verifiedAccount(true)
                .build();
        userRepository.save(newUser);

        UserAuthProviderEntity newProvider = UserAuthProviderEntity.builder()
                .provider(provider)
                .providerUserid(providerUserId)
                .providerEmail(email)
                .providerName(name)
                .providerPicture(picture)
                .user(newUser)
                .build();
        providerRepository.save(newProvider);

        setSessionUser(request, newUser, provider, picture);
        response.sendRedirect("/index.html");
    }

    /**
     * 修改後的 setSessionUser 方法
     * 邏輯：優先檢查 user 資料庫是否有 icon，若有則轉為 Base64，否則使用 provider 提供的圖片網址
     */
    private void setSessionUser(HttpServletRequest request, UserEntity user, String provider, String picture) {
        
        String finalPicture = picture; // 預設使用 Google/Line 傳來的圖片 URL

        // 判斷資料庫是否有圖片 (byte[] 是否存在且長度大於 0)
        if (user.getIcon() != null && user.getIcon().length > 0) {
            try {
                // 將 byte[] 轉為 Base64 字串
                String base64Icon = Base64.getEncoder().encodeToString(user.getIcon());
                
                // 加上 Data URI Scheme 前綴，讓前端可以直接放入 <img src="...">
                // 這裡預設為 png，瀏覽器通常能自動識別 jpeg 或 png
                finalPicture = "data:image/png;base64," + base64Icon;
                
            } catch (Exception e) {
                // 若轉換失敗，印出錯誤並維持使用原本的 picture (fallback)
                System.err.println("Icon Base64 conversion failed: " + e.getMessage());
            }
        }

        // 將最終決定的圖片 (Base64字串 或 URL) 放入 Session
        SessionUserDto sessionUser = SessionUserDto.from(user, provider, finalPicture);
        request.getSession().setAttribute("USER", sessionUser);
    }
}