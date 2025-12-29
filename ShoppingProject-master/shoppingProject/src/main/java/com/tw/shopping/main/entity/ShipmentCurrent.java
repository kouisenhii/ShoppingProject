package com.tw.shopping.main.entity;

import java.time.LocalDateTime;

import com.tw.shopping.main.enums.ShipmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table( name = "shipmentcurrent")
@Data
public class ShipmentCurrent {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name ="shipmentid", nullable = false)
	private Integer shipmentId;
	
	@Enumerated(EnumType.ORDINAL)
	@Column(name ="currentstatus", nullable = false)
	private ShipmentStatus shipmentStatus;
	
	@Column(name ="updatedtime", nullable = false)
	private LocalDateTime updateDtime;
	
	@OneToOne
	@JoinColumn(name ="orderid", nullable = false)
	private OrderEntity order;
	
}
