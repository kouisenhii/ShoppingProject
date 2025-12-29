package com.tw.shopping.main.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.tw.shopping.main.dto.OrderItemResponseDto;
import com.tw.shopping.main.dto.OrderUpdateRequestDto;
import com.tw.shopping.main.dto.WishListItemResponseDto;
import com.tw.shopping.main.dto.OrderResponseDto;
import com.tw.shopping.main.entity.OrderEntity;
import com.tw.shopping.main.entity.OrderItemEntity;
import com.tw.shopping.main.entity.ShipmentCurrent;
import com.tw.shopping.main.entity.WishListItem;


@Mapper(componentModel = "spring")
public interface OrderMapStruct {
	
	//查詢
	@Mapping(source = "shipCurrent.shipmentStatus", target =  "shipmentStatus")
	OrderResponseDto toResponseDto(OrderEntity Entity);
	
	
	@Mapping(source = "product.productid", target =  "productId")
	@Mapping(source =  "product.pname", target= "productName")
	@Mapping(target = "subTotal", expression = "java(Entity.getQuantity() * Entity.getUnitPrice())")
	OrderItemResponseDto toResponseDto(OrderItemEntity Entity);
	
	
//	ShipmentCurrentResponseDto toResponseDto(ShipmentCurrent Entity);
	WishListItemResponseDto toResponseDto(WishListItem Entity);
	
	List<OrderResponseDto> orderToResponseDto(List<OrderEntity> Entity);
	List<OrderItemResponseDto> orderItemToResponseDto(List<OrderItemEntity> Entity);
	
	@BeanMapping(ignoreByDefault = true) 
	@Mapping(target =  "orderAddress", source = "orderAddress")
	void updateEntityFromDto(OrderUpdateRequestDto updateDto, @MappingTarget OrderEntity entity);
}
