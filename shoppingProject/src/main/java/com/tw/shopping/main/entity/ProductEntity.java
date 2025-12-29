package com.tw.shopping.main.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
public class ProductEntity implements Serializable{
	private static final long serialVersionUID = 1L; // 版本號
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer productid;

    private String pname, description ,color, specification,  productimage;
    
    private Integer price, rating, stock;
    
    @CreationTimestamp
	@Column(name = "createdat", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
	
	@UpdateTimestamp
	@Column(name = "updatedat")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

    //	---------------------------------------------

    @ManyToOne
	@JoinColumn(name = "categoryid")
	@JsonBackReference
	private CategoryEntity category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private Set<CartEntity> cartItems;
    
    // 賴 新增的 11/30 
 // 讓前端接收 JSON 時，能看到 "categoryid": 1
    @JsonProperty("categoryid")
    public Integer getCategoryidView() {
        return category != null ? category.getCategoryid() : null;
    }

    // 讓前端送出 JSON (新增商品) 時，能自動把 "categoryid": 1 設定進 category 物件
    @JsonProperty("categoryid")
    public void setCategoryidView(Integer id) {
        if (id != null) {
            CategoryEntity c = new CategoryEntity();
            c.setCategoryid(id);
            this.category = c;
        }
    }


}
