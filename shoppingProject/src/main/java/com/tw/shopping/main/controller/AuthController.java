package com.tw.shopping.main.controller;

import com.tw.shopping.main.dto.AuthResponseDto;
import com.tw.shopping.main.dto.LocalUserLoginDto;
import com.tw.shopping.main.dto.LocalUserRegisterDto;
import com.tw.shopping.main.dto.SessionUserDto;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserRepository; // 新增：用於檢查 Email
import com.tw.shopping.main.service.LocalLoginService;
import com.tw.shopping.main.service.LocalRegisterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final LocalRegisterService localRegisterService;
    private final LocalLoginService localLoginService;
    private final UserRepository userRepository; // 注入 UserRepository

    /**
     * Local 註冊（含後端嚴格驗證）
     * * API：POST /api/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody LocalUserRegisterDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws IOException {

        // ==========================================
        // 🔒 後端安全檢查 (Security Check)
        // ==========================================
        HttpSession session = httpRequest.getSession();
        String savedCode = (String) session.getAttribute("email_code");
        String savedEmail = (String) session.getAttribute("email_for_verify");

        // 1. 檢查 Session 是否過期或未發送驗證碼
        if (savedCode == null || savedEmail == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "驗證碼已過期或未發送，請重新驗證"));
        }

        // 2. 檢查「註冊的 Email」是否等於「驗證碼寄送的 Email」
        // (防止使用者驗證了 A 信箱，卻拿驗證碼去註冊 B 信箱)
        if (!savedEmail.equalsIgnoreCase(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "註冊信箱與驗證信箱不符"));
        }

        // 3. 檢查驗證碼是否正確
        if (!savedCode.equals(request.getVerifyCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "驗證碼錯誤"));
        }

        // 4. 檢查 Email 是否已被註冊 (雙重檢查，防止 Service 層漏接)
        if (userRepository.existsByEmail(request.getEmail())) {
            // 這裡可以更細緻判斷是純 Local 重複還是 Third-party 衝突，但為了安全，先擋下
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "此 Email 已經被註冊，請直接登入"));
        }

        // ==========================================
        // ✅ 通過驗證，執行註冊邏輯
        // ==========================================

        UserEntity newUser = localRegisterService.register(request, httpRequest, httpResponse);

        // 如果 newUser 為 null，表示 Service 判定需要導向綁定頁面 (例如已存在 Google 帳號)
        // 注意：您的 Service 內部似乎做了 redirect，這裡回傳 ok() 會讓前端以為成功
        // 建議 Service 拋出異常，或者這裡回傳特殊狀態碼
        if (newUser == null) {
            // 假設 Service 已處理 redirect，這裡回傳特定訊息給前端處理
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("success", false, "message", "帳號需綁定，已導向"));
        }

        // 註冊成功後，清除 Session 中的驗證碼，防止重放攻擊
        session.removeAttribute("email_code");
        session.removeAttribute("email_for_verify");

        // 自動登入邏輯
        LocalUserLoginDto loginReq = new LocalUserLoginDto();
        loginReq.setEmail(request.getEmail());
        loginReq.setPassword(request.getPassword());

        SessionUserDto sessionUser = localLoginService.authenticateLocalUser(loginReq, httpRequest);

        return ResponseEntity.ok(
                AuthResponseDto.builder()
                        .userId(sessionUser.getUserId())
                        .name(sessionUser.getName())
                        .email(sessionUser.getEmail())
                        .provider(sessionUser.getProvider())
                        .avatar(sessionUser.getAvatar())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LocalUserLoginDto request,
            HttpServletRequest httpRequest) {

        try {
            SessionUserDto sessionUser = localLoginService.authenticateLocalUser(request, httpRequest);
            return ResponseEntity.ok(
                    AuthResponseDto.builder()
                            .userId(sessionUser.getUserId())
                            .name(sessionUser.getName())
                            .email(sessionUser.getEmail())
                            .provider(sessionUser.getProvider())
                            .avatar(sessionUser.getAvatar())
                            .role(sessionUser.getRole())
                            .build()
            );
        } catch (Exception e) {
            // 捕獲登入失敗異常 (如密碼錯誤)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("帳號或密碼錯誤");
        }
    }

    @GetMapping("/user/me")
    public ResponseEntity<AuthResponseDto> me(HttpServletRequest request) {
        SessionUserDto sessionUser = (SessionUserDto) request.getSession().getAttribute("USER");
        if (sessionUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                AuthResponseDto.builder()
                        .userId(sessionUser.getUserId())
                        .name(sessionUser.getName())
                        .email(sessionUser.getEmail())
                        .provider(sessionUser.getProvider())
                        .avatar(sessionUser.getAvatar())
                        .role(sessionUser.getRole())
                        .build()
        );
    }
}