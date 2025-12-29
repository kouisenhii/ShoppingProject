package com.tw.shopping.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Base64;

import com.tw.shopping.main.entity.UserEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionUserDto implements Serializable {

    /**
     * 使用者在 Session 中的必要資訊：
     *
     * userId          → 使用者唯一編號
     * name            → 顯示名稱
     * email           → 登入 Email
     * provider        → 登入來源（LOCAL / GOOGLE / LINE / FACEBOOK）
     * avatar          → 使用者頭像（Base64 或 URL）
     * verifiedAccount → 是否完成 Email 驗證
     *
     * 注意：
     * - SessionUser 不應保存敏感資料（密碼、token 等）
     * - SessionUser 為輕量的登入狀態載體，避免在 Session 儲存太多資料
     */
    private Long userId;
    private String name;
    private String email;
    private String provider;
    private String avatar;
    private Boolean verifiedAccount;
    private String role; // 賴 新增的 腳色

    private static final long serialVersionUID = 1L;


    /**
     * 將資料庫中的 BLOB 頭像轉成 Base64 字串
     *
     * 用於 LOCAL 帳號登入時：
     * - 若 user.icon 不為 null → 轉 Base64 後用 DataURI 顯示
     * - 若無頭像 → 回傳 null，交由呼叫端自行決定預設頭像
     *
     * 好處：
     * - 避免在 Session 儲存 byte[]（太大）
     * - 前端可直接 <img src="data:image/jpeg;base64,...">
     */
    public static String toBase64(byte[] data) {
        return (data != null && data.length > 0)
                ? Base64.getEncoder().encodeToString(data)
                : null;
    }


    /**
     * 建立 SessionUser 的 Local 專用版本
     *
     * 使用情境：
     * - Local 註冊成功 → 自動登入
     * - Local 帳密登入 → 驗證成功後呼叫
     *
     * avatar：
     * - 使用 DB icon 的 Base64
     * - 此方法專門處理 LOCAL，因此 provider = LOCAL
     */
    public static SessionUserDto fromEntity(UserEntity user) {

        String avatar = toBase64(user.getIcon());

        return SessionUserDto.builder()
                .userId(user.getUserid())
                .name(user.getName())
                .email(user.getEmail())
                .provider("LOCAL")
                .avatar(avatar)
                .verifiedAccount(user.getVerifiedAccount())
                .role(extractRole(user))
                .build();
    }


    /**
     * 建立 SessionUser 的第三方登入版本（Google / LINE / Facebook）
     *
     * providerPicture（第三方登入頭像 URL）
     * - 若第三方提供 picture → 使用 URL
     * - 若第三方未提供 → fallback 到 Local icon
     *
     * 使用情境：
     * - Google OAuth2 登录成功後呼叫
     * - Line Login 登入成功後呼叫
     * - Facebook Login（未來）亦可使用此方法
     *
     * 將所有 provider 的登入資料統一轉成 SessionUser，利於前端 UI 顯示。
     */
    public static SessionUserDto from(
            UserEntity user,
            String provider,
            String providerPicture
    ) {
        String avatar;

        if (providerPicture != null && !providerPicture.isEmpty()) {
            // 社群登入 → 使用第三方提供的頭像 URL
            avatar = providerPicture;
        } else {
            // 若第三方未提供頭像 → 使用本地端 icon Base64
            avatar = toBase64(user.getIcon());
        }

        return SessionUserDto.builder()
                .userId(user.getUserid())
                .name(user.getName())
                .email(user.getEmail())
                .provider(provider)
                .avatar(avatar)
                .verifiedAccount(user.getVerifiedAccount())
                .role(extractRole(user))
                .build();
    }
    
    // 賴 [新增] 輔助方法：從 UserEntity 取出第一個角色名稱
    private static String extractRole(UserEntity user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            return user.getRoles().iterator().next().getRoleName();
        }
        return "ROLE_MEMBER"; // 預設值
    }
}
