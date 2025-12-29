package com.tw.shopping.main.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.WishListItemAddRequestDto;
import com.tw.shopping.main.dto.WishListItemResponseDto;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.entity.WishListItem;
import com.tw.shopping.main.entity.WishListItemId;
import com.tw.shopping.main.exception.BusinessValidationException;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.mapper.WishListItemMapStruct;
import com.tw.shopping.main.repository.WishListRepo;
import com.tw.shopping.main.service.helper.WishListValidationHelper;
import com.tw.shopping.main.service.helper.WishListValidationHelper.WishListEntities;
import com.tw.shopping.main.util.SecurityUtility;

@Service
public class WishListItemService {
	
	private final WishListRepo wishListRepo; 
	private final SecurityUtility securityUtility;
	private final WishListValidationHelper helper;
	private final WishListItemMapStruct mapper;
	
	public WishListItemService(
			
			WishListRepo wishListRepo,
			SecurityUtility securityUtility,
			WishListValidationHelper helper,
			WishListItemMapStruct mapper) {
		
		this.wishListRepo = wishListRepo;
		this.securityUtility = securityUtility;
		this.helper = helper ;
		this.mapper = mapper ;
	}


//-------------------------------
//查詢
	public List<WishListItemResponseDto> getWishListItems() {
		
		Long currentId = securityUtility.getCurrentUserIdOrThrow();
		List<WishListItem> wishListItems = wishListRepo.findByUserIdFetch(currentId);
		
		
		return mapper.toResponseDto(wishListItems);	
	}
	

//增加
	public WishListItemResponseDto addToWishListItem(WishListItemAddRequestDto addDto) {
		
		WishListEntities result = helper.getValidatedEntities(addDto.getProductId());
		
		UserEntity user = result.userInfo();
		ProductEntity product = result.product();
		
		WishListItemId id = new WishListItemId(user.getUserid(), product.getProductid());
		
		if (wishListRepo.existsById(id)) {
            // 由於 Composite Key 已經存在
            throw new BusinessValidationException("商品已在收藏清單中，請勿重複新增!");
        }
		
		// 4. 創建並設定 WishListItem 實體
        WishListItem newItem = new WishListItem();
        newItem.setId(id);            // 設定複合主鍵
        newItem.setUserInfo(user);  // 設定 User 關聯
        newItem.setProduct(product); // 設定 Product 關聯
        newItem.setAddTime(LocalDateTime.now());// 必須在這裡設定時間
        // addTime 會自動設定 (假設 Entity 中已初始化)
        
        
        // 5. 執行持久化
        WishListItem savedItem = wishListRepo.save(newItem);
        
        // 6. 轉換並回傳 Response DTO
        return mapper.toResponseDto(savedItem); 
  
	}

//刪除
	public void removeFromWishListItem(Integer productId) {
		
		// 1. 安全性檢查：獲取當前用戶 ID
		Long currentUserId = securityUtility.getCurrentUserIdOrThrow();

        // 2. 建立複合主鍵 (WishListItemId)
        // 這是刪除的唯一條件，包含了 User ID 和 Product ID
        WishListItemId compositeId = new WishListItemId(currentUserId, productId);

        // 3. 檢查是否存在 (Existence Check - 404)
        // 這是必要的，否則 deleteById() 雖然會執行，但我們需要拋出明確的 404
        if (!wishListRepo.existsById(compositeId)) {
            // 如果項目不存在，拋出 ResourceNotFoundException (404)
            throw new ResourceNotFoundException("收藏清單中找不到商品!");
        }

        // 4. 執行刪除
        wishListRepo.deleteById(compositeId);
    
    }
		
}

