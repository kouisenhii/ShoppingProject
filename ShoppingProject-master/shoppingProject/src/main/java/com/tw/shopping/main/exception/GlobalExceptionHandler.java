package com.tw.shopping.main.exception;

import com.tw.shopping.main.dto.ErrorResponseDto;
import com.tw.shopping.main.dto.ErrorResponseDto2;
import com.tw.shopping.main.mapper.ErrorMapStruct;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 啟用全域異常捕獲
@RestControllerAdvice 
public class GlobalExceptionHandler {
	
	private final ErrorMapStruct mapper;
	
	public GlobalExceptionHandler(
			
			ErrorMapStruct mapper) {
		
		this.mapper = mapper ;
	}

    // =========================================================
    // 1. 處理 DTO 格式驗證失敗 (400 Bad Request)
    // 捕獲 @NotBlank, @Size, @Pattern 失敗時 Spring 拋出的異常
    // =========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        // 提取所有欄位錯誤，將 FieldName 和 Message 放入 Map
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // 返回 400 狀態碼 (BAD_REQUEST)
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); 
    }
    
    // =========================================================
    // 2. 處理 JSON 或日期格式解析錯誤 (400 Bad Request)
    // 捕獲 JSON 格式不正確、前導零問題、日期格式錯誤等
    // =========================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto2> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String friendlyMessage;
        
        Throwable rootCause = ex.getRootCause(); // 嘗試追溯到最根本的原因

        if (rootCause instanceof DateTimeParseException) {
            // 情況 1: 日期格式錯誤
            friendlyMessage = "日期欄位格式輸入錯誤!請確認日期格式為 YYYY-MM-DD (例如：2025-01-01)";
        } 
        // 檢查 Jackson 拋出的常見錯誤 (例如：非數字的輸入、前導零、布林值錯誤)
        else if (rootCause instanceof com.fasterxml.jackson.core.JsonParseException || 
                 rootCause instanceof com.fasterxml.jackson.databind.JsonMappingException) {
            
            String causeMessage = rootCause.getMessage().toLowerCase();
            
            if (causeMessage.contains("numeric value") || causeMessage.contains("leading zeroes")) {
                // 情況 2: 數值格式錯誤 (例如手機號碼傳了 "abc" 或 "09..." 但 DTO 設了 Long)
                friendlyMessage = "數值欄位格式錯誤!請確認手機號碼、ID 等欄位只輸入數字 ";
            } else {
                // 情況 3: 其他常見的 JSON 結構錯誤 (例如少了一個括號)
                friendlyMessage = "請檢查是否遺漏了必要的符號或值!";
            }
        } 
        else {
            // 情況 4: 其他未預期的讀取錯誤
            friendlyMessage = "請求數據格式無法識別!請檢查所有欄位值的類型是否正確!";
        }
        
        ErrorResponseDto2 errorDto = mapper.toErrorDto(
    	        HttpStatus.BAD_REQUEST.value(), 
    	        friendlyMessage, 
    	        "HTTP_MESSAGE_NOT_READABLE"
    	    );
        // 將原始錯誤打印到日誌，供後端除錯
        ex.printStackTrace(); 

        // 返回 400 狀態碼和更友善的訊息
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }
    
    // =========================================================
    // 3. 處理業務驗證失敗 (400 Bad Request)
    // 捕獲您自定義的 BusinessValidationException (如密碼不一致/Email 重複)
    // =========================================================
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponseDto2> handleBusinessValidation(BusinessValidationException ex) {
        // 返回 400 狀態碼和業務錯誤訊息
    	ErrorResponseDto2 errorDto = mapper.toErrorDto(
    	        HttpStatus.BAD_REQUEST.value(), 
    	        ex.getMessage(), 
    	        "BUSINESS_VALIDATION"
    	    );
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST); 
    }

    // =========================================================
    // 4. 處理資源找不到 (404 Not Found)
    // 捕獲您自定義的 ResourceNotFoundException
    // =========================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto2> handleResourceNotFound(ResourceNotFoundException ex) {
        // 返回 404 狀態碼和錯誤訊息
    	ErrorResponseDto2 errorDto = mapper.toErrorDto(
    	        HttpStatus.NOT_FOUND.value(), 
    	        ex.getMessage(), 
    	        "RESOURCE_NOT_FOUND"
    	    );
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND); 
    }
    
    // =========================================================
    // 處理所有 URL 或 Query 參數錯誤 (400 Bad Request)
 // 包含：類型不匹配、缺少 Query 參數、缺少 Path 變數
    // =========================================================
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class, 
        MissingServletRequestParameterException.class,
        MissingPathVariableException.class // 👈 新增這個例外
    })
    public ResponseEntity<ErrorResponseDto2> handleUrlParameterErrors(Exception ex) {
        
        String friendlyMessage;
        String errorCode;

        if (ex instanceof MethodArgumentTypeMismatchException) {
            
            // 類型不匹配錯誤：例如 /orders/abc (預期數字)
            friendlyMessage = "請求參數類型錯誤" ;
            errorCode = "METHOD_ARG_TYPE_MISMATCH";   
           
            
        } else if (ex instanceof MissingServletRequestParameterException
        		|| ex instanceof MissingPathVariableException) {   
            
            // 缺少 Query 參數錯誤：例如 /orders?id= (預期有值)
            friendlyMessage = "缺少必要的請求參數!";        
            errorCode = "MISSING_REQUEST_PARAM";
            
        } 
      
        else {
            friendlyMessage = "請求參數錯誤!";
            errorCode = "URL_PARAM_ERROR";
        }
        
        ErrorResponseDto2 errorDto = mapper.toErrorDto(
    	        HttpStatus.BAD_REQUEST.value(), 
    	        friendlyMessage, 
    	        errorCode);

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST); 
    }

      /**
     *  RuntimeException 一般錯誤 400
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException ex) {

    		ErrorResponseDto error = ErrorResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 【新增】專門處理庫存不足的異常
     * 配合組員的風格，統一回傳 ErrorResponseDto
     */
    @ExceptionHandler(StockNotEnoughException.class)
    public ResponseEntity<ErrorResponseDto> handleStockNotEnough(StockNotEnoughException ex) {
        
        // 使用組員定義的 Builder 來建立錯誤訊息
        ErrorResponseDto error = ErrorResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request
                .message(ex.getMessage())             // 這裡會顯示 "商品 [xxx] 庫存不足..."
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
