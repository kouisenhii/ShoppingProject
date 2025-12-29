package com.tw.shopping.main.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

	
	@Size(min = 2, max = 50, message = "姓名長度需 2 ~ 50 字")
	@Pattern(
	    regexp = "^[\\u4e00-\\u9fa5]{2,50}$",
	    message = "姓名必須皆是中文"
	)
	private String name;
	
	
	@Pattern(regexp = "^09[0-9]{8}$", message = "手機號碼必須為 09 開頭的 10 位數字")
	private String phone;
	
	
	@Past(message = "生日不可為未發生的日期")
	private LocalDate birthday;
	
	
	@Size(min = 6 , max = 255, message = "最少需填入6字")
	@Pattern( 
		regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s]{6,255}$",
	    	message = "地址必須包含中文，且長度在6到255個字元之間，可包含數字、英文")
	private String address;
	
	
	@Pattern(regexp = "^[MFO]$", message = "性別選擇無效，請選擇有效的值 ")
	private String gender;
	
	
	private Long userid;
	private String email;
	private String password;

}
