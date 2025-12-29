package com.tw.shopping.main.service.helper;



import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import com.tw.shopping.main.entity.UserEntity;


@Service
public class UserValidationHelper {
    
//    private final UserRepository userRepo;
//    
//    public UserValidationHelper(
//    		UserRepository userRepo) {
//    	
//    	this.userRepo = userRepo ;
//    	
//    }

    public boolean isProfileComplete(UserEntity user) {
        System.out.println("來ㄌ");
        // 1. 檢查所有 String 類型欄位 (Name, Phone, Address, Gender)
        boolean stringFieldsValid = 
            // StringUtils.hasText 是檢查: not null 且非空字串
            StringUtils.hasText(user.getName()) && 
            StringUtils.hasText(user.getPhone()) && 
            StringUtils.hasText(user.getAddress()) &&
            StringUtils.hasText(user.getGender()) &&
            StringUtils.hasText(user.getEmail());
            // 💡 註：passwordHash 是系統欄位，不需讓使用者填寫，不應包含在此檢查中。

        // 2. 檢查所有 Object/Date 類型欄位 (Birthday)
        // 只需要檢查物件本身是否為 null
        boolean objectFieldsValid = 
            user.getBirthday() != null; 
        
       
        // 3. 最終結果：所有條件都必須滿足
        return stringFieldsValid && objectFieldsValid;
        
    }
}