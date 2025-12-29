//package com.tw.shopping.main.dto;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//import com.tw.shopping.main.enums.ShipmentStatus;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ShipmentCurrentResponseDto {
//	
//	private Integer shipmentId;
//	
//	
//	private ShipmentStatus shipmentStatus;
//	private String getShipmentStatusDisplay() {
//        if (this.shipmentStatus == null) {
//            return null; // 處理 null 情況
//        }
//        return this.shipmentStatus.getDescription();
//    }
//	private LocalDateTime updateDtime;
//}
