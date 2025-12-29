package com.tw.shopping.main.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tw.shopping.main.dto.PasswordUpdateRequestDto;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.util.SecurityUtility;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;




@Service
public class PasswordCommandService {

	private final UserRepository userRepo;
	private final SecurityUtility securityUtility ;
    private final PasswordEncoder passwordEncoder;
    private final SessionManagementService sessionManagementService;

	
	
	public PasswordCommandService(
			
			UserRepository userRepo, 
			SecurityUtility securityUtility, 
			PasswordEncoder passwordEncoder,
			SessionManagementService sessionManagementService) {
		
		this.userRepo = userRepo ;
		this.securityUtility  = securityUtility ;
		this.passwordEncoder = passwordEncoder;
		this.sessionManagementService = sessionManagementService;
	}
	
	@Transactional
	public void updatePassword(PasswordUpdateRequestDto passwordRequest, HttpServletRequest req) {
		
		//水平權限確認並取得entity
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
		UserEntity user = userRepo.findById(currentId).orElseThrow(() -> new  ResourceNotFoundException("找不到使用者資訊或未授權"));
		
		
		final String requestedOldPassword = passwordRequest.getOldPassword();
		final String requestedNewPassword = passwordRequest.getNewPassword();
		final String dbHashPassword = user.getPassword();

		//前端輸入新密碼與前端輸入確認新密碼必須相同 (!搬dto)
		if(!requestedNewPassword.equals(passwordRequest.getConfirmNewPassword())) {
			throw new BusinessValidationException("新密碼與確認密碼不一致!");
		}
		
		//前端輸入舊密碼需與資料庫相同
		if(!passwordEncoder.matches(requestedOldPassword, dbHashPassword)){
			throw new BusinessValidationException("舊密碼錯誤!");
		}
		
		//前端輸入新密碼不可與資料庫相同
		if(passwordEncoder.matches(requestedNewPassword, dbHashPassword)) {
			throw new BusinessValidationException("新密碼與舊密碼相同!");
		}
		
		//加密
		String newHashPassword = passwordEncoder.encode(requestedNewPassword);
		//更新entity(手動set)
		user.setPassword(newHashPassword);
		//更新到資料庫
		userRepo.save(user);
		
		
		// 統一處理兩種 Session 失效
		HttpSession currentSession = req.getSession(false); 

		if (currentSession != null) {
		    // 1. 強制所有裝置的 Spring Security Session 失效
		    sessionManagementService.expireAllSessionsForUser(currentId);
		    
		    // 2. 清除當前裝置的業務 Session 屬性
		    currentSession.removeAttribute("USER");
		    
		    // ⭐ 【關鍵新增】 強制銷毀當前瀏覽器持有的 Session
		    try {
		        currentSession.invalidate(); 
		    } catch (IllegalStateException e) {
		        // 如果 Session 已經被 SessionRegistry 標記為失效，這裡可能會拋出異常，捕獲即可
		        System.out.println("DEBUG: 當前 Session 已失效或已被銷毀。");
		    }
	}

}
	}
