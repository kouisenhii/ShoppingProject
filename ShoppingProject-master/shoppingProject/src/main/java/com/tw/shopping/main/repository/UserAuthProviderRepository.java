package com.tw.shopping.main.repository;

import com.tw.shopping.main.entity.UserAuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProviderEntity, Long> {

    /**
     * 根據 provider 與 providerUserId 查詢綁定記錄
     *
     * 用途：
     *  - 第三方登入時確認此使用者是否「已經綁定過」
     *  - Google Login：providerUserId = Google sub
     *  - Line Login：providerUserId = Line userId (sub)
     *  - Facebook Login：providerUserId = Facebook id (若有)
     *
     * 使用場景：
     *  - LINE Login Step B：找到綁定 → 直接登入
     *  - Google Login SuccessHandler：找到即代表已有帳號
     */
    Optional<UserAuthProviderEntity> findByProviderAndProviderUserid(String provider, String providerUserid);

    /**
     * 用 provider 與 Email 查詢綁定記錄
     *
     * 用途：
     *  - 若某 email 曾使用相同 provider 註冊（例如用 Google 註冊）
     *  - 可用來阻擋重複註冊相同 provider
     *
     * 使用場景：
     *  - Local 註冊時先檢查此 email 是否被 Google、LINE 使用
     */
    Optional<UserAuthProviderEntity> findByProviderAndProviderEmail(String provider, String providerEmail);

    /**
     * 用 email 查詢是否有任一 provider 使用此 email
     *
     * 用途：
     *  - 快速判斷某 email 是否已經用某種方式註冊（LOCAL / GOOGLE / LINE）
     *  - 常用於「輸入 email 後預先檢查」
     *
     * 使用場景：
     *  - LoginVerificationController：使用者輸入 email → 回傳是否為第三方帳號
     */
    Optional<UserAuthProviderEntity> findByProviderEmail(String providerEmail);

    /**
     * 查詢某一個 user（userId）綁定的所有 provider 列表
     *
     * 用途：
     *  - 查看一個帳號目前綁定了哪些第三方登入方式
     *
     * 使用場景：
     *  - 用於使用者中心（帳號綁定管理）
     *  - 開發綁定/解除綁定功能時使用
     */
    List<UserAuthProviderEntity> findAllByUserUserid(Long userid);

    /**
     * 取得所有使用相同 email 的 Provider 記錄
     *
     * 用途：
     *  - 取得所有 providerEmail = 某 email 的綁定資料（可能是 LOCAL / GOOGLE / LINE）
     *
     * 使用場景：
     *  - Local 註冊時，用此查詢所有 provider → 判斷是否已被其他第三方綁定
     *  - 決定是否要阻擋「Local 註冊」或提示使用 Google / LINE 登入
     */
    List<UserAuthProviderEntity> findAllByProviderEmail(String providerEmail);
}
