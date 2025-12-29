package com.tw.shopping.main.service.helper;

import org.springframework.stereotype.Service;

import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.exception.ResourceNotFoundException;
import com.tw.shopping.main.repository.ProductRepository;

import com.tw.shopping.main.repository.UserRepository;
import com.tw.shopping.main.util.SecurityUtility;

@Service
public class WishListValidationHelper {
	
	private final UserRepository userRepo;
	private final ProductRepository productRepo;
	private final SecurityUtility securityUtility;
	

	public WishListValidationHelper(
			
			UserRepository userRepo,
			ProductRepository productRepo,
			SecurityUtility securityUtility) {
		
		this.userRepo = userRepo;
		this.productRepo = productRepo;
		this.securityUtility = securityUtility;
	}
	

	
	
//  取得product entity跟WishListItem entity	
	public WishListEntities getValidatedEntities(Integer productId) {
		
		Long currentUserId = securityUtility.getCurrentUserIdOrThrow();
	    
	    // 1. 獲取 User
	    UserEntity userInfo = userRepo.findById(currentUserId)
	        .orElseThrow(() -> new ResourceNotFoundException("找不到使用者或無權存取"));
	    
	    // 2. 獲取 Product
	    ProductEntity product = productRepo.findById(productId)
	        .orElseThrow(() -> new ResourceNotFoundException("找不到商品或無權存取"));
	        
	    // 3. 返回兩個外鍵實體
	    return new WishListEntities(userInfo, product);
	}
	
	public record WishListEntities (UserEntity userInfo, ProductEntity product) {}
	
	
}
