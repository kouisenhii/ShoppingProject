package com.tw.shopping.main.dto;

import lombok.Data;

@Data
public class CheckoutRequestDto {
	// 用途 接收接收前端結帳請求
    private Long userId;
    private String address; // 配送地址
    private String paymentMethod; // 付款方式 (雖然目前強制綠界，但保留擴充性)
    // 綠界物流新增
    private String logisticsType;    
    private String logisticsSubType; 
    private String storeId;          
    private String storeName;       
}