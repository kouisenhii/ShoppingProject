package com.tw.shopping.main.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.annotations.RequireProfileComplete;
import com.tw.shopping.main.dto.PasswordUpdateRequestDto;
import com.tw.shopping.main.dto.UserIconResponseDto;
import com.tw.shopping.main.dto.UserIconUploadRequestDto;
import com.tw.shopping.main.dto.UserResponseDto;
import com.tw.shopping.main.dto.UserUpdateRequestDto;
import com.tw.shopping.main.service.PasswordCommandService;
import com.tw.shopping.main.service.UserCommandService;
import com.tw.shopping.main.service.UserQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

//@CrossOrigin
@RestController
@RequestMapping("/v1/userinfos")
public class UserController {

	private final UserQueryService userQueryService;
	private final UserCommandService userCommandService;
	private final PasswordCommandService passwordCommandService;
	
	public UserController(
			UserQueryService userQueryService, 
			UserCommandService userCommandService, 
			PasswordCommandService passwordCommandService) {
		
		this.userQueryService = userQueryService;
		this.userCommandService = userCommandService;
		this.passwordCommandService = passwordCommandService;
		
	}
	
//	顯示頭像
	@GetMapping("/icon")
	public ResponseEntity<UserIconResponseDto> getIcon() {
		
		return ResponseEntity.ok(userQueryService.getIcon());	
	}
	
	
//	依userid找個人資料
	@GetMapping	
	public ResponseEntity<UserResponseDto> findUserInfoById() {
		
		return ResponseEntity.ok(userQueryService.findUserInfoByUserId());
	}
	

	
//	修改頭像
	@RequireProfileComplete
	@PatchMapping("/icon/new")
	public ResponseEntity<UserIconResponseDto> updateIcon(
			
			@RequestBody UserIconUploadRequestDto requestDto) {
		
		return ResponseEntity.ok(userCommandService.updateIcon(requestDto));
	}
	
	
//	修改個人資料(不包含密碼)
	@PatchMapping("/personalinfo")
	public ResponseEntity<UserResponseDto> updateInfo(
			
			@Valid@RequestBody UserUpdateRequestDto userRequest) {
		
		return ResponseEntity.ok(userCommandService.updateInfo(userRequest));
	}
	
	
//	修改密碼
	@RequireProfileComplete
	@PatchMapping("/password")
	public ResponseEntity<Void> updatePassword(
			
			@Valid@RequestBody PasswordUpdateRequestDto passwordRequest,
			HttpServletRequest request) {
		
		passwordCommandService.updatePassword(passwordRequest, request);
		
		return ResponseEntity.noContent().build();
	}
	
}
