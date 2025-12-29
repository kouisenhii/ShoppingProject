package com.tw.shopping.main.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "role")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleid") // 對應全小寫
    private Integer roleId;

    @Column(name = "rolename", unique = true) // 對應全小寫
    private String roleName;
    
   
}
