package com.tw.shopping.main.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LocalUserRegisterDto {

	// ----------- name -----------
	@NotBlank(message = "姓名不能為空")
	@Size(min = 2, max = 50, message = "姓名長度需 2 ~ 50 字")
	@Pattern(
	    regexp = "^[\\u4e00-\\u9fa5]{2,50}$",
	    message = "姓名必須皆是中文"
	)
	private String name;

    // ----------- Email -----------
    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    private String email;
    
    // ----------- code（新增） -----------
    @NotBlank(message = "驗證碼不能為空")
    private String verifyCode;
    
    // ----------- password -----------
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 20, message = "密碼長度需 8 ~ 20 字")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])[A-Za-z0-9]{8,20}$",
        message = "密碼必須至少 1 個大寫和 1 個小寫字母，並且只能包含英文字母和數字"
    )
    private String password;

    // ----------- confirmPassword -----------
    @NotBlank(message = "確認密碼不能為空")
    private String confirmPassword;
    
    private Boolean bind; // 是否要綁定已有帳號

}
