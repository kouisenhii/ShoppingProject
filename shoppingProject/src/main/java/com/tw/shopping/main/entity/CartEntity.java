package com.tw.shopping.main.entity;

import java.time.LocalDateTime;

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
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart")
@Data
@NoArgsConstructor
public class CartEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartid;

    // 外鍵關聯到 UserEntity (userinfo 表)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    @JsonIgnore 
    private UserEntity user;

    // 外鍵關聯到 ProductEntity (product 表)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productid", nullable = false)
    @JsonIgnore 
    private ProductEntity product; 

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime addtime = LocalDateTime.now();

    // 常用於創建新項目時的構造函數
    public CartEntity(UserEntity user, ProductEntity product, Integer quantity) {
        this.user = user;
        this.product = product;
        this.quantity = quantity;
        this.addtime = LocalDateTime.now();
    }
    
}
