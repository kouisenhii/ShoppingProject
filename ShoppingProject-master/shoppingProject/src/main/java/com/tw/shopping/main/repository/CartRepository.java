package com.tw.shopping.main.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.dto.CartItemView;
import com.tw.shopping.main.entity.CartEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.entity.UserEntity;


public interface CartRepository extends JpaRepository<CartEntity, Long>{
	Optional<CartEntity> findByUserAndProduct(UserEntity user, ProductEntity product);
	@Query("SELECT SUM(c.quantity) FROM CartEntity c WHERE c.user.userid = :userId")
    Long countTotalItemsByUserId(Long userId);
	List<CartEntity> findByUser(UserEntity user);
	
	
	
	@Query(value = """
		SELECT 
		    c.userid,
		    c.cartid,
		    c.quantity,
		    p.productid,
		    p.pname,
		    p.description,
		    p.price,
		    p.productimage,
		    p.color,
		    p.specification,
		    u.address 
		FROM 
			shopping.cart AS c
		JOIN 
			shopping.product AS p ON c.productid = p.productid
		JOIN 
			shopping.userinfo AS u ON c.userid = u.userid
		WHERE 
		    c.userid = :userId
		ORDER BY 
		    c.cartid DESC;
			""", nativeQuery = true)
	List<CartItemView> findCartItemByUserId(@Param("userId")Long userId);
	
	@Query("SELECT c FROM CartEntity c JOIN FETCH c.product WHERE c.user = :user")
    List<CartEntity> findByUserWithProducts(UserEntity user);
}


