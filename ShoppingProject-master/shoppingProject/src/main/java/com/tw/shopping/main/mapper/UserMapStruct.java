package com.tw.shopping.main.mapper;

import java.util.Base64;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.tw.shopping.main.dto.UserIconResponseDto;
import com.tw.shopping.main.dto.UserIconUploadRequestDto;
import com.tw.shopping.main.dto.UserResponseDto;
import com.tw.shopping.main.dto.UserUpdateRequestDto;
import com.tw.shopping.main.entity.UserEntity;


/**
 * MapStruct 介面：用於處理 UserInfo Entity 與 DTO 之間的轉換。
 * componentModel = "spring": 讓 Spring 容器可以自動管理並注入這個 Mapper。
 * nullValuePropertyMappingStrategy = IGNORE: 這是實現「部分更新」的關鍵，
 * 如果 DTO 中的欄位為 null (即前端未傳送)，則忽略，不會覆蓋 Entity 中的舊值。
 */
@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapStruct {
		
	
    /**
     * 1. Entity 轉 Response DTO (查詢操作)
     * 從資料庫讀取的 UserInfo 轉換為回傳給前端的 UserResponseDto。
     * * @param entity 來源：UserInfo Entity
     * @return UserResponseDto 目標：回傳給前端的 DTO
     */
	//回傳個人資料
	
    UserResponseDto toResponseDto(UserEntity entity);

    @AfterMapping
    default void mapIsThirdPartyLogin(@MappingTarget UserResponseDto dto, UserEntity entity) {
        // 判斷 UserEntity 的 password 欄位是否為 null 或空字串
        // null 表示從未設定本地密碼 (標準 OAuth 建立)
        // 實務上，空字串也應視為無密碼
        dto.setIsThirdPartyLogin(entity.getPassword() == null || entity.getPassword().isEmpty());
    }

    
    
    //回傳頭像
    @Mapping(source = "icon", target = "iconBase64", qualifiedByName = "bytesToBase64")
    UserIconResponseDto toIconResponseDto(UserEntity entity);

    
    /**
     * 2. 更新操作：將 Request DTO 的數據應用到現有的 Entity 上
     * * @MappingTarget UserInfo entity: 表示這是目標物件，應對其進行修改。
     * @param dto 來源：前端傳入的 UserUpdateRequestDto (只包含要修改的欄位)
     * @param entity 目標：從資料庫查詢出來的現有 UserInfo Entity
     */
    
    
    //更新個人資料
    @Mapping(target = "userid", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "icon", ignore = true)
    //    MapStruct 會自動生成正確的代碼，僅更新 DTO 裡非 null 的欄位（name, phone 等）。
    void updateEntityFromDto(UserUpdateRequestDto dto, @MappingTarget UserEntity entity);
    
    
    
    //更新頭像
    @Mapping(source = "iconBase64", target = "icon", qualifiedByName = "base64ToBytes")
    void updateIconEntityFromDto(UserIconUploadRequestDto dto, @MappingTarget UserEntity entity);
   
//    -----------------------------------------
    
    //上傳
  	 @Named("base64ToBytes")
  	    default byte[] decodeBase64(String base64String) {
  	        if (base64String == null || base64String.isEmpty()) {
  	            return null; // 處理空值或空字串
  	        }
  	        // Base64 解碼的核心邏輯
  	        return Base64.getDecoder().decode(base64String);
  	    }
  	 
  	 
  	 //用於回應 DTO
  	 @Named("bytesToBase64")
  	     default String encodeBase64(byte[] iconBytes) {
  	         if (iconBytes == null) {
  	             return null;
  	         }
  	         return Base64.getEncoder().encodeToString(iconBytes);
  	     }
   
}