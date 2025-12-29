package com.tw.shopping.main.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.entity.WishListItem;
import com.tw.shopping.main.entity.WishListItemId;

public interface WishListRepo extends JpaRepository<WishListItem, WishListItemId>{
		
	@Query("""
		    select w from WishListItem w
		    join fetch w.product
		    where w.userInfo.userid = :userId 
		    """)
		List<WishListItem> findByUserIdFetch(@Param("userId") Long currentId);

	
	
	Optional<WishListItem> findByUserInfo_UseridAndProduct_Productid(Integer userId, Integer productId);
}
