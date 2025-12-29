package com.tw.shopping.main.controller;

import java.util.List;
import com.tw.shopping.main.service.OrderCommandService;
import com.tw.shopping.main.service.OrderQueryService;

import jakarta.validation.Valid;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.annotations.RequireProfileComplete;
import com.tw.shopping.main.dto.OrderItemResponseDto;
import com.tw.shopping.main.dto.OrderResponseDto;
import com.tw.shopping.main.dto.OrderUpdateRequestDto;


@CrossOrigin
@RestController
@RequestMapping("/v1/orders")
public class OrderController2 {

    
	private final OrderQueryService orderQueryService;
	private final OrderCommandService orderCommandService;

	public OrderController2(
			
			OrderQueryService orderQueryService,
			OrderCommandService orderCommandService) {
		
		this.orderQueryService = orderQueryService;
		this.orderCommandService = orderCommandService;		
	}
	
	
//	依 userId 取得所有訂單
    @GetMapping
	public ResponseEntity<List<OrderResponseDto>> findOrders() {
    	
    		return ResponseEntity.ok(orderQueryService.findOrdersByUserId());
	}
    
    
//	依 orderId  取得單筆訂單(擋你)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto>  findOrderByOrderId (
    		
    			@PathVariable Integer orderId) {
    	
    		return ResponseEntity.ok(orderQueryService.findOrderByOrderId(orderId));
    }
    
    
    
//   依 orderId  取得訂單清單
    @GetMapping("/{orderId}/orderitems")
    public ResponseEntity<List<OrderItemResponseDto> > findOrderItems(
    		
    			@PathVariable Integer orderId){

    		return ResponseEntity.ok(orderQueryService.findOrderItemByOrderId(orderId));
    }
    
    
// 查詢貨品運送狀態
//    @GetMapping("/{orderId}/shipments")
//    public ResponseEntity<ShipmentCurrentResponseDto>  getShipmentStatus(
//    		
//    			@PathVariable Integer orderId) {
//    	
//    		return ResponseEntity.ok(orderQueryService.getShipmentStatus(orderId));
//    }
//    
//
//    
    
////	修改訂單地址
//    @RequireProfileComplete
//    @PatchMapping("/{orderId}/orderAddress")
//    public ResponseEntity<OrderResponseDto> updateOrderAddress(
//    		
//    		@Valid@RequestBody OrderUpdateRequestDto updateDto, 
//    		@PathVariable Integer orderId) {
//    	
//    	return ResponseEntity.ok(orderCommandService.updateOrderAddress(updateDto, orderId));
//    }
    
// 取消訂單
    @RequireProfileComplete
    @PatchMapping("/{orderId}/cancelOrder")
    public ResponseEntity<Void> cancelOrder(
    		
    			@PathVariable Integer orderId) {
    	
    		orderCommandService.cancelOrder(orderId);
    		return ResponseEntity.noContent().build();
    }
    
//// 退貨
//    @RequireProfileComplete
//    @PatchMapping("/{orderId}/returnGoods")
//    public ResponseEntity<Void> returnGoods(
//    		
//    		@PathVariable Integer orderId) {
//    	
//    	orderCommandService.returnGoods(orderId);
//    	return ResponseEntity.noContent().build();
//    }
}
