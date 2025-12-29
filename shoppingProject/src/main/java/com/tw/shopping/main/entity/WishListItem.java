package com.tw.shopping.main.entity;


import java.time.LocalDateTime;



import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "wishlist")
@Data
public class WishListItem {
	
	@EmbeddedId
	private WishListItemId id;
	
	
	@Column(name ="addtime")
	private LocalDateTime addTime;
	
	@ManyToOne
	@MapsId("userId")
	@JoinColumn(name = "userid", nullable = false)
	private UserEntity userInfo;
	
	@ManyToOne
	@MapsId("productId")
	@JoinColumn(name = "productid", nullable = false)
	private ProductEntity product;
	
}
