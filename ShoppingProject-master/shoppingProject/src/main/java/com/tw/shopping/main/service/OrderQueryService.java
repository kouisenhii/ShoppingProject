package com.tw.shopping.main.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.tw.shopping.main.dto.OrderItemResponseDto;
import com.tw.shopping.main.dto.OrderResponseDto;


import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.OrderItemEntity;
import com.tw.shopping.main.entity.ShipmentCurrent;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.mapper.OrderMapStruct;
import com.tw.shopping.main.repository.OrderItemRepository;
import com.tw.shopping.main.repository.OrderRepository;
import com.tw.shopping.main.repository.ProductRepository;
import com.tw.shopping.main.service.helper.OrderValidationHelper;
import com.tw.shopping.main.service.helper.OrderValidationHelper.OrderValidationResult;
import com.tw.shopping.main.util.SecurityUtility;


@Service
public class OrderQueryService {

	private final OrderRepository oRepo;
	private final OrderItemRepository orderItemRepo;
	private final ProductRepository productRepo;
	private final SecurityUtility securityUtility ;
	private final OrderValidationHelper helper;
	private final OrderMapStruct mapper;

	
    public OrderQueryService(
    		
    		OrderRepository orderRepository, 
    		OrderItemRepository orderItemRepo,
    		ProductRepository productRepo,
    		SecurityUtility securityUtility,
    		OrderValidationHelper helper,
    		OrderMapStruct mapper) {
  
        this.oRepo = orderRepository;
        this.orderItemRepo = orderItemRepo; 
        this.productRepo = productRepo;
        this.securityUtility = securityUtility;
        this.helper = helper ;
        this.mapper = mapper ; 
    }
//	-------------------------------------
	
	
 // 1. 根據當前會員id查詢訂單s (修改過)
    public List<OrderResponseDto> findOrdersByUserId() {

        Long currentId = securityUtility.getCurrentUserIdOrThrow();
        List<OrderEntity> orderEntities = oRepo.findByUserid_Userid(currentId);

        // 先用 Mapper 轉成 DTO List
        List<OrderResponseDto> dtos = mapper.orderToResponseDto(orderEntities);

        // 【關鍵修改】手動填入 paymentMethod
        // 因為 Entity 是 paymentmethods (有s)，DTO 是 paymentMethod (沒s)，
        // 且資料庫有混雜資料 (1, 5, Credit_CreditCard)，我們直接原樣傳給前端處理
        for (int i = 0; i < orderEntities.size(); i++) {
            OrderEntity entity = orderEntities.get(i);
            OrderResponseDto dto = dtos.get(i);
            
            // 把 Entity 的值塞給 DTO
            dto.setPaymentMethod(entity.getPaymentmethods());
        }

        return dtos;
    }

    // 2. 根據訂單id查詢訂單 (修改過)
    public OrderResponseDto findOrderByOrderId(Integer orderId) {

        Long currentId = securityUtility.getCurrentUserIdOrThrow();

        OrderEntity o = oRepo.findByOrderIdAndUserid_Userid(orderId, currentId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到訂單或無權存取"));

        // 先轉成 DTO
        OrderResponseDto dto = mapper.toResponseDto(o);
        
        // 【關鍵修改】手動填入 paymentMethod
        dto.setPaymentMethod(o.getPaymentmethods());

        return dto;
    }

	  
	//  根據訂單id查詢訂單明細
	  public List<OrderItemResponseDto> findOrderItemByOrderId(Integer orderId){
			  
		  Long currentId = securityUtility.getCurrentUserIdOrThrow();
		
		  oRepo.findByOrderIdAndUserid_Userid(orderId, currentId)
		  	.orElseThrow(() -> new ResourceNotFoundException("找不到訂單或無權存取"));
		  
		  List<OrderItemEntity> oItems = orderItemRepo.findByOrderIdFetchProduct(orderId); 
		  
		
		  return  mapper.orderItemToResponseDto(oItems);
	  } 
}

//	 // 查詢貨品運送狀態
//	  public ShipmentCurrentResponseDto getShipmentStatus(Integer orderId) {
//		 
//		  OrderValidationResult result = helper.getValidatedOrderEntities(orderId);
//		  
//		  ShipmentCurrent shipmentCurrent = result.shipment();
//		 
//		  return mapper.toResponseDto(shipmentCurrent);
//	  }
//		  
