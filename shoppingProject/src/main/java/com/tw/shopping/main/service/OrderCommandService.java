package com.tw.shopping.main.service;


import org.springframework.stereotype.Service;

import com.tw.shopping.main.annotations.RequireProfileComplete;
import com.tw.shopping.main.dto.OrderResponseDto;
import com.tw.shopping.main.dto.OrderUpdateRequestDto;

import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.OrderItemEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.mapper.OrderMapStruct;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.service.helper.OrderValidationHelper;
import com.tw.shopping.main.service.helper.OrderValidationHelper.OrderValidationResult;
import jakarta.transaction.Transactional;

@Service
public class OrderCommandService {
//  0=CREATED;1=PAID ;2=CANCELLED;3=REFUNDED 
	
	private final OrderRepository oRepo;
	private final ProductRepository productRepo;
	private final OrderMapStruct mapper;
	private final OrderValidationHelper helper;
	
	public OrderCommandService (
			
			OrderRepository oRepo, 
			ProductRepository productRepo,
			OrderMapStruct mapper,
			OrderValidationHelper helper) {
		
		this.oRepo = oRepo;
		this.productRepo = productRepo;
		this.mapper = mapper ;
		this.helper = helper;
	}
//	--------------------------------


////  修改訂單地址
//	@Transactional
//	@RequireProfileComplete
//	public OrderResponseDto updateOrderAddress(OrderUpdateRequestDto updateDto,Integer orderId) {
//		
//		OrderValidationResult result = helper.getValidatedOrderEntities(orderId);
//		helper.ensureIsModifiable(result);
//		
//		OrderEntity verifiedO= result.order();
//		//前端資料回來
//		mapper.updateEntityFromDto(updateDto, verifiedO);
//		//persist到資料庫
//		OrderEntity newO = oRepo.save(verifiedO);
//		//轉成responseDto回傳，確認拿的是更新過的資料
//		return mapper.toResponseDto(newO);		
//	}
//	
	
//  取消訂單
	@Transactional
	@RequireProfileComplete
	public OrderResponseDto cancelOrder(Integer orderId) {
		
		OrderValidationResult result = helper.getValidatedOrderEntities(orderId);
		helper.ensureIsModifiable(result);
		
		OrderEntity canceledO = result.order();
		//直接回補(預設不論有無付款皆扣庫存)
		replenishStock(canceledO);
		
		//付錢了就退款，沒付就
		if (canceledO.getOrderStatus() == OrderStatus.PAID) {
	       //還沒寫
	    }
		
		//更改資料庫內訂單狀態為已取消
		canceledO.setOrderStatus(OrderStatus.CANCELLED);
		OrderEntity newO = oRepo.save(canceledO);
	    
	    return mapper.toResponseDto(newO);
		
	}
	
////  退貨
//	@Transactional
//	@RequireProfileComplete
//	public OrderResponseDto returnGoods(Integer orderId) {
//
//		OrderValidationResult result = helper.getValidatedOrderEntities(orderId);
//		helper.ensureReturnable(result);
//		
//		OrderEntity refundO = result.order();
//		
//	    //回補庫存
//		replenishStock(refundO);
//		
//	    // 退款
//		if (refundO.getOrderStatus() == OrderStatus.PAID) {
//		       //還沒寫
//		    }
//
//	    // 訂單狀態變更
//		refundO.setOrderStatus(OrderStatus.RETURNED); // 例如變更為 '退貨申請中'
//	    
//	    // 儲存變更 
//		 OrderEntity newO = oRepo.save(refundO);
//	     return mapper.toResponseDto(newO);	
//	}
//	
  //---------------------------------------
	
//  庫存回補用
	private void replenishStock(OrderEntity order) {
	   
	    
	    // 在 @Transactional 內操作，確保原子性
	    for (OrderItemEntity item : order.getOrderItems()) {
	        ProductEntity product = item.getProduct(); 
	        product.setStock(product.getStock() + item.getQuantity());
	        productRepo.save(product); 
	    }
	}
	
}


	