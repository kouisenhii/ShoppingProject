package com.tw.shopping.main.service;

import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.UserIconResponseDto;
import com.tw.shopping.main.dto.UserIconUploadRequestDto;
import com.tw.shopping.main.dto.UserResponseDto;
import com.tw.shopping.main.dto.UserUpdateRequestDto;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.mapper.UserMapStruct;
import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.util.SecurityUtility;

import jakarta.transaction.Transactional;


@Service
public class UserCommandService {
	
	private final UserRepository userRepo;
	private final SecurityUtility securityUtility;
	private final UserMapStruct mapper;
	
	
	public UserCommandService(
			
			UserRepository userRepo,  
			SecurityUtility securityUtility,
			UserMapStruct mapper) {
		
		this.userRepo = userRepo;
		this.securityUtility = securityUtility ;
		this.mapper = mapper ;
	}
	
//	---------------------------
	
	
//修改頭像
	@Transactional
	public UserIconResponseDto updateIcon(UserIconUploadRequestDto requestDto) {
		
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
		UserEntity user = userRepo.findById(currentId)
				.orElseThrow(() -> new ResourceNotFoundException("找不到使用者資訊或未授權!"));
		
		//直接修改user這個entity的內容
		mapper.updateIconEntityFromDto(requestDto, user);
		
		UserEntity newUser = userRepo.save(user);
		
		return mapper.toIconResponseDto(newUser);
		
	}
	
	
	
//修改個人資料
	@Transactional
	public UserResponseDto updateInfo(UserUpdateRequestDto requestDto) {	
		
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
		UserEntity user = userRepo.findById(currentId)
				.orElseThrow(() -> new ResourceNotFoundException("找不到使用者資訊或未授權!"));
		
		
		if (requestDto.getEmail() != null ||
				requestDto.getUserid() != null ) {
		        // 拋出業務異常，明確告知不允許修改敏感資料
		        throw new BusinessValidationException("不允許嘗試修改電子郵件或使用者ID!");
		    }
		
		if (requestDto.getPassword() != null) { 
		        // 拋出業務異常，明確告知不允許修改敏感資料
		        throw new BusinessValidationException("不可在個人資料修改處嘗試修改密碼!");
		    }
		
		//直接修改user這個entity的內容
		mapper.updateEntityFromDto(requestDto, user);

		UserEntity newUser = userRepo.save(user);

		return mapper.toResponseDto(newUser);
	}
	
	
	

}
