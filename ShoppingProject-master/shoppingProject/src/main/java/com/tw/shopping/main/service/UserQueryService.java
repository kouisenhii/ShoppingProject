package com.tw.shopping.main.service;

import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.UserIconResponseDto;
import com.tw.shopping.main.dto.UserResponseDto;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.mapper.UserMapStruct;
import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.util.SecurityUtility;

@Service
public class UserQueryService {
	
	private final UserRepository userRepo;
	private final SecurityUtility securityUtility;
	private final UserMapStruct mapper;
	
	public UserQueryService(
			
			UserRepository userRepo, 
			SecurityUtility securityUtility, 
			UserMapStruct mapper) {
		
		this.userRepo = userRepo;
		this.securityUtility = securityUtility;
		this.mapper = mapper ;
	}
	
//	------------------------------
	
	
//查詢頭像
	public UserIconResponseDto getIcon() {
				
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
				
		UserEntity user = userRepo.findById(currentId)
					.orElseThrow(() -> new ResourceNotFoundException("找不到使用者資訊或未授權"));
			

		return mapper.toIconResponseDto(user);
	}
	
	
//查詢使用者資料
	public UserResponseDto findUserInfoByUserId() {
		
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
		
		UserEntity user = userRepo.findById(currentId)
				.orElseThrow(() -> new ResourceNotFoundException("找不到使用者資訊或未授權!"));
	
		return mapper.toResponseDto(user);
	}
	

	
}
