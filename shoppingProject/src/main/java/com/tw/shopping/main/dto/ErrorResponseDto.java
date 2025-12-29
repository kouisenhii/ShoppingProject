package com.tw.shopping.main.dto;

import lombok.Data;
import lombok.Builder;

/**
 * ErrorResponse
 *
 * 此 DTO 用於統一後端錯誤回應格式，讓前端可依固定格式處理錯誤訊息。
 *
 * 設計目的：
 *  - 讓所有錯誤回傳都有一致的 JSON 結構
 *  - 便於前端顯示錯誤提示或彈窗
 *  - 便於全域錯誤處理器（ExceptionHandler）統一產生錯誤回應
 *
 * 欄位說明：
 *  - status  → HTTP 狀態碼（例如 400, 401, 404, 500...）
 *  - message → 錯誤描述（給前端顯示的錯誤訊息）
 *
 * 常見使用場景：
 *  - 使用者註冊/登入失敗（密碼錯誤 / email 已存在）
 *  - 驗證碼驗證失敗
 *  - 欄位驗證錯誤（後端校驗）
 *  - 未登入直接訪問需要驗證的 API
 */
@Data
@Builder
public class ErrorResponseDto {

    private int status;
    private String message;
}
