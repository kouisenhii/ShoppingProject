package com.tw.shopping.main.controller;

import com.tw.shopping.main.service.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/forget")
@RequiredArgsConstructor
public class ForgetPasswordController {

    private final EmailService emailService;

    // Regex 定義
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String CODE_REGEX = "^\\d{6}$";

    /**
     * 發送「忘記密碼驗證碼」
     */
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestParam String email,
                                           HttpSession session) {
        // 格式驗證
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email:Email 不能為空");
        }
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            return ResponseEntity.badRequest().body("Email:Email 格式不正確");
        }

        // 發送邏輯
        emailService.sendVerificationCode(email, session);
        return ResponseEntity.ok("驗證碼已寄出");
    }

    /**
     * 驗證「忘記密碼驗證碼」
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String email,
                                    @RequestParam String code,
                                    HttpSession session) {
        
        // 格式驗證
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email:Email 不能為空");
        }
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            return ResponseEntity.badRequest().body("Email:Email 格式不正確");
        }

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("驗證碼:驗證碼不能為空");
        }
        if (!Pattern.matches(CODE_REGEX, code)) {
            return ResponseEntity.badRequest().body("驗證碼:驗證碼必須是 6 位數字");
        }

        // 業務邏輯驗證
        boolean ok = emailService.verifyCode(email, code, session);

        if (!ok) {
            return ResponseEntity.badRequest().body("驗證碼:驗證碼錯誤或已失效");
        }

        session.setAttribute("forget_verified", true);
        session.setAttribute("forget_email", email);

        return ResponseEntity.ok("驗證成功");
    }
}