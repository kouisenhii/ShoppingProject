package com.tw.shopping.main.controller;

import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.service.EmailService;

import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class RegisterVerificationController {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;

    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    /**
     * 寄送 Email 註冊驗證碼
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendRegisterCode(
            @RequestParam String email,
            HttpSession session
    ) {
        // 1. 基本檢查
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email 不能為空");
        }
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            return ResponseEntity.badRequest().body("Email 格式不正確");
        }

        // 2. 嚴格檢查：Email 是否已存在
        boolean existsInUser = userRepository.existsByEmail(email);
        List<UserAuthProviderEntity> providers = userAuthProviderRepository.findAllByProviderEmail(email);
        boolean isLocal = providers.stream().anyMatch(p -> p.getProvider().equals("LOCAL"));

        if (existsInUser || isLocal) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("此 Email 已經註冊過，請直接登入");
        }

        // 3. 寄送驗證碼
        try {
            emailService.sendRegisterVerificationCode(email, session);
            return ResponseEntity.ok("驗證碼已寄出");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("寄送失敗，請稍後再試");
        }
    }

    /**
     * 檢查 Email 狀態 (前端 Check Email API 用)
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {

        boolean existsInUser = userRepository.existsByEmail(email);
        
        List<UserAuthProviderEntity> providersInDB =
                userAuthProviderRepository.findAllByProviderEmail(email);

        List<String> providers = providersInDB.stream()
                .map(UserAuthProviderEntity::getProvider)
                .toList();

        if (!existsInUser && providers.isEmpty()) {
            return ResponseEntity.ok(Map.of("exists", false, "providers", List.of()));
        }

        return ResponseEntity.ok(Map.of("exists", true, "providers", providers));
    }
}