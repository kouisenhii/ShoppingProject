package com.tw.shopping.main.controller;

import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/forget")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 定義與前端一致的密碼強度 Regex (8-20字，需包含大小寫英文)
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\\d]{8,20}$";

    /**
     * 重設密碼 API
     * API: POST /api/forget/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request,
                                           HttpSession session) {

        // 1. 安全性檢查：驗證使用者是否已通過 Email 驗證碼確認
        Boolean verified = (Boolean) session.getAttribute("forget_verified");
        String email = (String) session.getAttribute("forget_email");

        if (verified == null || !verified || email == null) {
            // 若 session 遺失或未完成驗證，回傳 403 Forbidden
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("操作逾時或未經驗證，請重新執行忘記密碼流程");
        }

        String newPassword = request.get("newPassword");

        // 2. 密碼格式驗證 (新增部分)
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("新密碼不能為空");
        }
        // 嚴格比對 Regex，防止繞過前端驗證
        if (!Pattern.matches(PASSWORD_REGEX, newPassword)) {
            return ResponseEntity.badRequest().body("密碼格式錯誤 (需 8-20 字，含大小寫英文字母)");
        }

        // 3. 查詢使用者
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("找不到此用戶");
        }

        UserEntity user = userOpt.get();

        // 4. 更新密碼 (使用 BCrypt 加密)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 5. 清除忘記密碼相關 Session，防止重複使用
        session.removeAttribute("forget_verified");
        session.removeAttribute("forget_email");
        session.removeAttribute("email_verification_code"); // 若有殘留也一併清除
        
        // 6. 回傳成功
        return ResponseEntity.ok("密碼重設成功");
    }
}