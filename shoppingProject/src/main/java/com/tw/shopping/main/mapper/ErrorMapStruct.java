package com.tw.shopping.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tw.shopping.main.dto.ErrorResponseDto2;

@Mapper(componentModel = "spring")
public interface ErrorMapStruct {
	/**
     * 處理標準業務錯誤 (RNF, BVE)
     */
    @Mapping(target = "message", source = "message")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "errorCode", source = "code")
    ErrorResponseDto2 toErrorDto(int status, String message, String code);
    
    /**
     * 處理驗證失敗 (MethodArgumentNotValidException)
     * 這需要複雜的邏輯，通常需要手動寫程式碼或用 @Context 傳遞 BindingResult
     */
    // 為了簡潔，這裡通常會讓 GHE 直接處理 Map<String, String> 的轉換，不完全依賴 MapStruct
}
	


