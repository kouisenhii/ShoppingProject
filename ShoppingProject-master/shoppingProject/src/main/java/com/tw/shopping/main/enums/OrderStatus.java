package com.tw.shopping.main.enums;


import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
	
	CREATED("已成立"),
	PAID("已付款"),
	RETURNED("已退貨"),
	CANCELLED("已取消"),
	COMPLETE("已完成"),
	PENDING("未付款");
	
	private final String description;
	
	
	//確保交換資料不會出問題
	public static OrderStatus fromString(String status)  {
	    if (status == null || status.isBlank()) {
	        throw new BusinessValidationException("狀態字串不能為空!");
	    }
	    try {
	        return OrderStatus.valueOf(status.toUpperCase());
	    } catch (IllegalArgumentException e) {
	        throw new ResourceNotFoundException("無效的訂單狀態!" ); 
	    }
	}
}


