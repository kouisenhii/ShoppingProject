package com.tw.shopping.main.controller;

import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginVerificationController {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        List<UserAuthProviderEntity> providersInDB = providerRepository.findAllByProviderEmail(email);
        List<String> providers = providersInDB.stream()
                .map(UserAuthProviderEntity::getProvider)
                .toList();

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        // 判斷是否為本地帳號 (有密碼)
        boolean isLocalAccount = userOpt.isPresent()
                && userOpt.get().getPassword() != null
                && !userOpt.get().getPassword().isBlank();

        List<String> mergedProviders = new ArrayList<>(providers);
        if (isLocalAccount && !mergedProviders.contains("LOCAL")) {
            mergedProviders.add("LOCAL");
        }

        boolean exists = !mergedProviders.isEmpty();

        return ResponseEntity.ok(
                Map.of(
                        "exists", exists,
                        "providers", mergedProviders
                )
        );
    }
}