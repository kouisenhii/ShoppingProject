package com.tw.shopping.main.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tw.shopping.main.dto.WishListItemResponseDto;
import com.tw.shopping.main.entity.WishListItem;

@Mapper(componentModel = "spring")
public interface WishListItemMapStruct {

    @Mapping(source = "product.productid", target = "productId")
    @Mapping(source = "product.pname", target = "productName")
    @Mapping(source = "product.productimage", target = "productImage")
    @Mapping(source =  "product.price", target = "price")
    @Mapping(source = "product.description", target = "description")
    @Mapping(source = "product.color", target = "color")
    @Mapping(source = "product.specification", target = "specification")
    @Mapping(source = "addTime", target = "addTime")
    WishListItemResponseDto toResponseDto(WishListItem entity);

    List<WishListItemResponseDto> toResponseDto(List<WishListItem> list);
}
