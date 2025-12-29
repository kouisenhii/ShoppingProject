package com.tw.shopping.main.service.helper;


import org.springframework.stereotype.Service;

import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.ShipmentCurrent;
import com.tw.shopping.main.enums.OrderStatus;
import com.tw.shopping.main.enums.ShipmentStatus;
import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.repository.OrderRepository;

import com.tw.shopping.main.repository.ShipmentCurrentRepo;
import com.tw.shopping.main.util.SecurityUtility;


@Service
public class OrderValidationHelper {
	
	private final OrderRepository oRepo;
	private final ShipmentCurrentRepo shipmentRepo;
	private final SecurityUtility securityUtility;
	

	public OrderValidationHelper(
			
			OrderRepository oRepo,
			ShipmentCurrentRepo shipmentRepo,
			SecurityUtility securityUtility) {
		
		this.oRepo = oRepo;
		this.shipmentRepo = shipmentRepo;
		this.securityUtility = securityUtility;
	}
//
////  檢查存取權限
//	public void ensureOrderOwner(Order o,Integer currentid) {
//		if(!o.getUserInfo().getUserId().equals(currentid)) {
//			  throw new AccessDeniedException("無權查詢此訂單明細"); 
//		  }
//	}
//	
//	
////  檢查訂單是否存在
//	public Order findOrdersOrThrow(Integer orderId) {
//		return oRepo.findById(orderid).orElseThrow(
//				  () -> new ResourceNotFoundException("查無此訂單 !(ID: " + orderid + ")"));
//	}
//}
	
//  取得order entity跟shipmentCurrent entity
	public OrderValidationResult getValidatedOrderEntities(Integer orderId) {
	    
	    // 1. 水平驗證：取得當前用戶 ID
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
	    
	    // 2. 訂單存在與所有權驗證 (Order + Ownership)
	    OrderEntity order = oRepo.findByOrderIdAndUserid_Userid(orderId, currentId)
	        .orElseThrow(() -> new ResourceNotFoundException("找不到訂單或無權存取"));
	    
	    // 3. 取得物流狀態
	    ShipmentCurrent shipment = shipmentRepo.findShipmentCurrentByOrder_OrderId(orderId)
	        .orElseThrow(()->new ResourceNotFoundException("找不到商品出貨狀態"));
	        
	    // 4. 返回結果 (使用一個簡單的內部 Class 或 Record 來打包結果)
	    return new OrderValidationResult(order, shipment);
	}

	// 內部類別用於打包結果
	public record OrderValidationResult (OrderEntity order, ShipmentCurrent shipment) {}

	//	------------------------------
	
//  判斷訂單狀態及貨品出貨狀態(修改地址及取消訂單)
	public void ensureIsModifiable(OrderValidationResult result) {
	        
	        // 1. 檢查訂單狀態 (OrderStatus Enum)
	        if (result.order().getOrderStatus() != OrderStatus.CREATED && 
	        	    result.order().getOrderStatus() != OrderStatus.PAID) {
	        	
	            throw new BusinessValidationException("訂單已取消、完成退貨或完成，無法操作");
	        }
	        
	        // 2. 檢查物流狀態 (ShipmentStatus Enum)
	        if (result.shipment().getShipmentStatus().isShipped()) {
	            throw new BusinessValidationException("訂單已出貨，僅可執行退貨");
	        }
	        
	    }

//	判斷退貨

	public void ensureReturnable(OrderValidationResult result) {
	    
	    // 1. 檢查訂單是否為最終狀態 (SHARED LOGIC - 消除冗餘)
	    if (result.order().getOrderStatus() != OrderStatus.CREATED && 
	        result.order().getOrderStatus() != OrderStatus.PAID) {
	        
	        throw new BusinessValidationException("訂單已取消、完成退貨或完成，無法操作");
	    }
	
	    // 2. 檢查物流狀態 (RETURN SPECIFIC LOGIC - 業務規則)
	    // 確保已出貨，或者檢查是否為待出貨狀態（避免用退貨流程處理取消）
	    if (result.shipment().getShipmentStatus() == ShipmentStatus.PENDING_SHIPMENT) {
	        throw new BusinessValidationException("訂單狀態為待出貨，可直接取消訂單");
	    }
	    
	    
	}
}
