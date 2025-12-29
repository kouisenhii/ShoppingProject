package com.tw.shopping.main.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.repository.UserRepository;

import com.tw.shopping.main.service.helper.UserValidationHelper;
import com.tw.shopping.main.util.SecurityUtility;


@Aspect
@Component
public class ProfileCheckAspect {
    
    // 注入你需要的 Service/Repo
	private final UserRepository userRepo;
    private final SecurityUtility securityUtility;
    private final UserValidationHelper helper;
    
    
    public ProfileCheckAspect(
    		UserRepository userRepo,
    		SecurityUtility securityUtility,
    		UserValidationHelper helper) {
    	
    	this.userRepo = userRepo;
    	this.securityUtility = securityUtility;
    	this.helper = helper;
    	
    }

    // 這是 Before Advice (前置通知)：在目標方法執行前運行
    @Before("@annotation(com.tw.shopping.annotations.RequireProfileComplete)")
    public void checkProfileCompletion(JoinPoint joinPoint) {
        
        // 1. 獲取當前用戶 ID
        // 如果未登入，SecurityUtility 會拋出 InsufficientAuthenticationException (401)
        Long currentUserId = securityUtility.getCurrentUserIdOrThrow(); 
        
        // 2. 獲取 UserInfo 實體
        UserEntity user = userRepo.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("找不到使用者資訊或未授權"));

        
        // 3. 執行核心檢查
        if (!helper.isProfileComplete(user)) { // <--- 呼叫 UserInfo 內的方法
            // 拋出業務錯誤，讓 Global Handler 捕捉為 400 Bad Request
            throw new BusinessValidationException("請先填寫完整個人資料才可進行其他操作!");
        }
    }
}

