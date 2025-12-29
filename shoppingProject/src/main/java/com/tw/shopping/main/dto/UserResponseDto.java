package com.tw.shopping.main.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
	
	private Long userid;
	private String name;
	private String email;
	private String phone;
	private LocalDate birthday;
	private String address;
	private String gender;
	private Boolean isThirdPartyLogin;

}
