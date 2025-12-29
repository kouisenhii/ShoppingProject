package com.tw.shopping.main.controller;

import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.dto.SessionUserDto;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class BindAccountController {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;

    /**
     * 【綁定頁面資料載入 API】
     *
     * 前端進入 bind_account.html 後會呼叫此 API。
     * 功能：
     * 1. 從 Session 中取出「待綁定的第三方帳號資料」(PENDING_BIND_xxx)
     * 2. 查出該 email 使用者目前已有的登入方式（例如：LOCAL、GOOGLE、LINE）
     * 3. 整合成 JSON 回傳前端，用於畫面顯示：
     *    - 左邊：此次登入的第三方資訊（pending）
     *    - 右邊：已綁定的 provider 清單（existingProviders）
     *
     * 若 Session 無任何 pending 資料 → 視為非法或流程過期 → 回傳 400
     */
    @GetMapping("/pending-info")
    public ResponseEntity<?> pendingInfo(HttpServletRequest req) {
        // 取得 Session（若不存在則直接拒絕）
        HttpSession session = req.getSession(false);

        if (session == null) return ResponseEntity.badRequest().build();
        System.out.println("pending-info Session B = " + session.getId());
        // 從 Session 取出此次綁定的 email
        String email = (String) session.getAttribute("PENDING_BIND_EMAIL");
        if (email == null) return ResponseEntity.badRequest().body("No pending data");
        // 查找本地使用者
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");
        // 查詢該使用者已綁定的所有 provider（例如：LOCAL + GOOGLE）
        List<UserAuthProviderEntity> providers =
                providerRepository.findAllByUserUserid(user.getUserid());
        // 將 provider entity 轉換成前端可直接顯示的格式
     // BindAccountController.java (修改第 61 行到第 70 行)
        List<Map<String, Object>> existingProviders = providers.stream()
            .map(p -> {
                // ⭐ 使用 Map.ofEntries 或 HashMap 處理 null 值
                return Map.<String, Object>of(
                    "provider", providerToDisplayName(p.getProvider()),
                    "rawProvider", p.getProvider(),
                    "email", p.getProviderEmail() != null ? p.getProviderEmail() : "",
                    "name", p.getProviderName() != null ? p.getProviderName() : "",
                    // ❗ 尤其注意可能為 null 的欄位
                    "picture", p.getProviderPicture() != null ? p.getProviderPicture() : ""
                );
                
                // 警告：如果 p.getProvider() 傳回 null，這裡仍然會因 providerToDisplayName 拋錯。
                // 如果您不確定 p.getProvider() 是否為 null，請使用以下更安全的寫法：
                /*
                Map<String, Object> map = new HashMap<>();
                String provider = p.getProvider();
                
                map.put("provider", providerToDisplayName(provider));
                map.put("rawProvider", provider);
                map.put("email", p.getProviderEmail()); // HashMap 允許 null
                map.put("name", p.getProviderName());
                map.put("picture", p.getProviderPicture());
                return map;
                */
            })
            .toList();

        // 回傳完整資料，用於綁定 UI 顯示
        return ResponseEntity.ok(
                Map.of(

                        // 本次登入的第三方資訊（即將綁定的 provider）
                        "pending", Map.of(
                                "name", session.getAttribute("PENDING_BIND_NAME"),
                                "picture", session.getAttribute("PENDING_BIND_PICTURE"),
                                "email", email,
                                "provider", providerToDisplayName(
                                        (String) session.getAttribute("PENDING_BIND_PROVIDER")
                                ),
                                "rawProvider", session.getAttribute("PENDING_BIND_PROVIDER")
                        ),

                        // 使用者目前已綁定的所有 provider
                        "existingProviders", existingProviders
                )
        );
    }

    /**
     * 【綁定動作 API】
     *
     * 使用者在 bind_account.html 按下「確認綁定」後呼叫此 API。
     * 功能：
     * 1. 從 Session 中取出 PENDING_BIND_xxx（第三方 provider 的資料）
     * 2. 查本地使用者
     * 3. 寫入 user_auth_provider（建立綁定關係）
     * 4. 將使用者登入（建立 SessionUser）
     * 5. 清除 pending 資料（避免重複綁定與資安問題）
     */
    @PostMapping("/bind-provider")
    public ResponseEntity<?> bindProvider(HttpServletRequest req) {

        HttpSession session = req.getSession(false);
        if (session == null) return ResponseEntity.badRequest().build();

        // 從 Session 取出綁定流程所需資料
        String email = (String) session.getAttribute("PENDING_BIND_EMAIL");
        String name = (String) session.getAttribute("PENDING_BIND_NAME");
        String picture = (String) session.getAttribute("PENDING_BIND_PICTURE");
        String provider = (String) session.getAttribute("PENDING_BIND_PROVIDER");
        String providerUserId = (String) session.getAttribute("PENDING_BIND_PROVIDER_USERID");

        if (email == null) {
            return ResponseEntity.badRequest().body("尚無綁定資訊");
        }

        // 查出本地帳號 entity
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("找不到使用者"));

        // 建立新的 provider 綁定關係
        UserAuthProviderEntity p = UserAuthProviderEntity.builder()
                .user(user)
                .provider(provider)
                .providerUserid(providerUserId)
                .providerEmail(email)
                .providerName(name)
                .providerPicture(picture)
                .build();

        providerRepository.save(p);

        // 綁定完成後直接視為登入，設置登入 Session
        SessionUserDto sessionUser = SessionUserDto.from(user, provider, picture);
        req.getSession().setAttribute("USER", sessionUser);

        // 清空綁定流程資料
        clearPending(session);

        return ResponseEntity.ok("綁定成功");
    }

    /**
     * 將 provider 代碼轉換為前端顯示用文字（例如：GOOGLE → Google 登入）
     */
    private String providerToDisplayName(String provider) {
        return switch (provider) {
            case "LOCAL" -> "一般登入";
            case "GOOGLE" -> "Google 登入";
            case "LINE" -> "Line 登入";
            default -> provider;
        };
    }

    /**
     * 清除所有 PENDING_BIND_xxx Session 內容
     * 避免：
     * - 重複綁定
     * - 使用者跳頁後仍保留敏感資訊
     * - 流程錯亂
     */
    private void clearPending(HttpSession session) {
        session.removeAttribute("PENDING_BIND_EMAIL");
        session.removeAttribute("PENDING_BIND_NAME");
        session.removeAttribute("PENDING_BIND_PICTURE");
        session.removeAttribute("PENDING_BIND_PROVIDER");
        session.removeAttribute("PENDING_BIND_PROVIDER_USERID");
    }
}
