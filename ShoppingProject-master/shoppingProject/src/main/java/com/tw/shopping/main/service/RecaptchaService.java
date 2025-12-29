package com.tw.shopping.main.service;

import com.tw.shopping.main.dto.RecaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.url}")
    private String recaptchaUrl;

    private final RestTemplate restTemplate;

    public RecaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateToken(String recaptchaToken) {
        // 如果 Token 為空，直接驗證失敗
        if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
            return false;
        }

        // 準備請求參數 (表單格式)
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", recaptchaSecret);
        map.add("response", recaptchaToken);

        try {
            // 發送 POST 請求給 Google
            RecaptchaResponse response = restTemplate.postForObject(
                    recaptchaUrl, map, RecaptchaResponse.class);

            // 回傳 Google 的判定結果 (true = 通過, false = 失敗)
            return response != null && response.isSuccess();
            
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 發生異常時視為驗證失敗
        }
    }
}