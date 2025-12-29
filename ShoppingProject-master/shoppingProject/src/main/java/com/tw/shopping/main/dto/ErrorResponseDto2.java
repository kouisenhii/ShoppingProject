package com.tw.shopping.main.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto2 {
	
	private String message;
    private int status;
    private String errorCode; 
    
//    private final Map<String, String> fieldErrors;

}
