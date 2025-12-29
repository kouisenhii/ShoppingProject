package com.tw.shopping.main.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalUserLoginDto {

    // ----------- Email -----------
    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    private String email;

    // ----------- password -----------
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 20, message = "密碼長度需 8 ~ 20 字")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])[A-Za-z0-9]{8,20}$",
        message = "密碼必須至少 1 個大寫、1 個小寫，並且只能包含英數字"
    )
    private String password;

}
