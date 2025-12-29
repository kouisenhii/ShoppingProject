package com.tw.shopping.main.entity;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "category")
@Data
public class CategoryEntity implements Serializable{
	private static final long serialVersionUID = 1L; // 版本號
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer categoryid;

    private String cname, code, categoryimage;

    private Integer parentid;

//	------------------------

    @OneToMany(mappedBy = "category")
	@JsonIgnore
	private List<ProductEntity> products;
}
