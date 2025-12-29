package com.tw.shopping.main.controller;

import java.util.List;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.annotations.RequireProfileComplete;
import com.tw.shopping.main.dto.WishListItemAddRequestDto;
import com.tw.shopping.main.dto.WishListItemResponseDto;
import com.tw.shopping.main.service.WishListItemService;


@RestController
@RequestMapping("/v1/wishList")
public class WishListController {
	
	
	private final WishListItemService wishListItemService;
	
	public WishListController(
			WishListItemService wishListItemService) {
		this.wishListItemService = wishListItemService;
	}
	
	
//	------------------------
	//查詢
		@GetMapping
		public ResponseEntity<List<WishListItemResponseDto>> getWishListItem() {
			List<WishListItemResponseDto> responseDto = wishListItemService.getWishListItems();
			return ResponseEntity.ok(responseDto);
			
		}
		

	//增加
		@RequireProfileComplete
		@PostMapping("/items")
		public ResponseEntity<WishListItemResponseDto> addToWishListItem(
				
				@RequestBody WishListItemAddRequestDto addDto) {
			
			WishListItemResponseDto responseDto = wishListItemService.addToWishListItem(addDto);
			return ResponseEntity.ok(responseDto);
			
		}

	//刪除
		@RequireProfileComplete
		@DeleteMapping("/items/{productId}")
		public ResponseEntity<Void> removeFromWishListItem(
				
				@PathVariable Integer productId) {
			
			wishListItemService.removeFromWishListItem(productId);
			return ResponseEntity.noContent().build();
			
		}

}
