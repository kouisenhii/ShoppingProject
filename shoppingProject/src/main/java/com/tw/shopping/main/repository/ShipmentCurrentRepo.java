package com.tw.shopping.main.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tw.shopping.main.entity.ShipmentCurrent;

public interface ShipmentCurrentRepo extends JpaRepository<ShipmentCurrent, Integer> {
	Optional<ShipmentCurrent> findShipmentCurrentByOrder_OrderId(Integer orderId);
}
