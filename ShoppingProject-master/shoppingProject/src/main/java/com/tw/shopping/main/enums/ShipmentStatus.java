package com.tw.shopping.main.enums;


import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShipmentStatus {
	
	PENDING_SHIPMENT("待出貨"),
	SHIPPED("已出貨"),
	DELIVERED("運送中"),
	ARRIVED("已送達");
	
	private final String description;
	
	
	//不是待出貨就都出貨了
	public boolean isShipped() {
        return this != PENDING_SHIPMENT;
    }
	
	
	//確保交換資料不會出問題
	public static ShipmentStatus fromString(String status)  {
	    if (status == null || status.isBlank()) {
	        throw new BusinessValidationException("狀態字串不能為空");
	    }
	    try {
	        return ShipmentStatus.valueOf(status.toUpperCase());
	    } catch (IllegalArgumentException e) {
	        throw new ResourceNotFoundException("無效的貨態狀態: " + status, e); 
	    }
	}
}
