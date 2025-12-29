package com.tw.shopping.main.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.entity.OrderItemEntity;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Integer>{

	@Query("SELECT oi FROM OrderItemEntity oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItemEntity> findByOrderIdFetchProduct(@Param("orderId") Integer orderId);
}
