package com.tw.shopping.main.entity;



import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "orderitem")
@Data
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itemid", nullable = false)
    private Integer itemId;

    @Column(name = "productid", nullable = false)
    private Long productId;


    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unitprice", nullable = false)
    private Integer unitPrice;

    @Column(name = "discount", nullable = false)
    private BigDecimal discount;




    @ManyToOne
    @JoinColumn(name ="orderid", nullable = false)
    @JsonIgnore
    private OrderEntity order; // 有做修改
    
  
    
    //----------------------------------------
    //賴 加的
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "productid", referencedColumnName = "productid", insertable = false, updatable = false)
    @JsonIgnore
    private ProductEntity product;

}