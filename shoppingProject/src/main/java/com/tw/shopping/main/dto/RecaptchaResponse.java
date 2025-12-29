package com.tw.shopping.main.dto;

import java.util.List;

public class RecaptchaResponse {
    private boolean success;
    private String challenge_ts; // timestamp
    private String hostname;
    private List<String> errorCodes; // 錯誤代碼 (選填)

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    // 其他 getter/setter 可以省略，我們主要只需要 success
}