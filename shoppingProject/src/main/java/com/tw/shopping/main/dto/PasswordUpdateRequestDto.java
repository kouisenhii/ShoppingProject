package com.tw.shopping.main.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateRequestDto {
	
	
	@NotBlank(message = "舊密碼不能為空")
    @Size(min = 8, max = 16, message = "密碼長度需介於 8 到 16 位之間") // 限制長度
	private String oldPassword;
	
	
	@NotBlank(message = "新密碼不能為空")
	@Size(min = 8, max = 20, message = "密碼長度需 8 ~ 20 字")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])[A-Za-z0-9]{8,20}$",
        message = "密碼必須至少 1 個大寫和 1 個小寫字母，並且只能包含英文字母和數字"
    )
	private String newPassword;
	

	@NotBlank(message = "確認新密碼不能為空")
	@Size(min = 8, max = 20, message = "密碼長度需 8 ~ 20 字")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])[A-Za-z0-9]{8,20}$",
        message = "密碼必須至少 1 個大寫和 1 個小寫字母，並且只能包含英文字母和數字"
    )
	private String confirmNewPassword;
	
}
