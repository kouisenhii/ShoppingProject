package com.tw.shopping.main.service;

import com.tw.shopping.main.dto.GoogleUserLoginDto;
import com.tw.shopping.main.dto.LineUserLoginDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CustomOAuth2UserService
 * --------------------------------------------------------------
 * 整合型 Service：同時處理 Google 和 LINE 的使用者資訊取得。
 * * 原理：
 * 1. 呼叫 super.loadUser() 讓 Spring 去跟第三方拿原始資料。
 * 2. 判斷 provider 是 "google" 還是 "line"。
 * 3. 封裝成對應的 DTO。
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        // 1. 取得原始資料 (Spring 內建實作)
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. 判斷是哪一家廠商 (轉大寫方便比對: GOOGLE / LINE)
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        
        // 3. 根據廠商封裝 DTO
        if ("GOOGLE".equals(registrationId)) {
            return new GoogleUserLoginDto(
                    registrationId,
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name"),
                    (String) attributes.get("picture"),
                    attributes
            );
         // 在 CustomOAuth2UserService.java 處理 LINE 時
        } else if ("LINE".equals(registrationId)) {
            return new LineUserLoginDto(
                    registrationId,
                    (String) attributes.get("userId"), // ⭐ 這裡要對應 properties 設定的 userId
                    (String) attributes.get("email"),  // 注意：Email 需要有申請權限才會回傳
                    (String) attributes.get("displayName"), // LINE 的名稱欄位叫做 displayName
                    (String) attributes.get("pictureUrl"),  // LINE 的圖片欄位叫做 pictureUrl
                    attributes
            );
        }

        // 預設回傳原始物件 (防呆)
        return oAuth2User;
    }
}