package com.tw.shopping.main.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.enums.ShipmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
	
//訂單編號 下單日期	總金額	訂單狀態	付款方式
	private String orderId;
	private LocalDate orderDate;
	private Integer totalAmount;
	
	private OrderStatus orderStatus;
	public String getOrderStatusDisplay() {
        if (this.orderStatus == null) {
            return null; // 處理 null 情況
        }
        return this.orderStatus.getDescription();
    }
	
	private String orderAddress;
	
	private ShipmentStatus shipmentStatus;
	public String getShipmentStatusDisplay() {
        if (this.shipmentStatus == null) {
            return null; // 處理 null 情況
        }
        return this.shipmentStatus.getDescription();
    }
	
	private String paymentMethod;
}
