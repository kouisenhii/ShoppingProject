package com.tw.shopping.main.entity;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tw.shopping.main.enums.OrderStatus;

import jakarta.persistence.CascadeType;

//import com.tw.shopping.enums.OrderStatus;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "`order`")
@Data
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderid", nullable = false)
    private Integer orderId;

    @Column(name = "orderdate", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "totalamount", nullable = false)
    private Integer totalAmount;
    
     

//    目前專案裡沒有這個方法 我先自己寫一個 到時候在改!!!!!
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private OrderStatus orderStatus;
    
    @Column(name = "orderaddress", nullable = false)
    private String orderAddress;
    @Column(name = "paymentmethods", nullable = false)
    private String paymentmethods;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private UserEntity userid;
//
    @OneToMany(mappedBy = "order")
    @JsonIgnore
    @ToString.Exclude
    private List<OrderItemEntity> orderItems = new ArrayList<>();
	
    @JsonIgnore
    @ToString.Exclude
	@OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
	private ShipmentCurrent shipCurrent;

//    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
//    private ShipmentCurrent shipCurrent;
    
    // ----------------------------------------------------------------
    // 【新增】綠界金流相關欄位 (Start)
    // ----------------------------------------------------------------
    @Column(name = "payment_confirm_date")
    private Date paymentconfirmdate;
    
    @Column(name = "ecpay_trade_no")
    private String ecpaytradeno;
    
    @Column(name = "ecpay_rtn_code")
    private String ecpayrtncode;
    
    @Column(name = "ecpay_rtn_msg")
    private String ecpayrtnmsg;
    
    @Column(name = "payment_status")
    private String paymentstatus;
    // 賴 新增的 11/29
    // ----------------------------------------------------------------
    // 【新增】綠界金流相關欄位 (stop)
    // ----------------------------------------------------------------
    
    // ----------------------------------------------------------------
    // 【新增】綠界物流相關欄位 (Start)
    // ----------------------------------------------------------------

    @Column(name = "logistics_type")
    private String logisticsType; // 物流類型 (HOME, CVS)

    @Column(name = "logistics_sub_type")
    private String logisticsSubType; // 物流子類型 (UNIMART, FAMI...)

    @Column(name = "store_id")
    private String storeId; // 門市代號

    @Column(name = "store_name")
    private String storeName; // 門市名稱

    @Column(name = "store_address")
    private String storeAddress; // 門市地址 (如果選超商，這是超商地址)

    // ----------------------------------------------------------------
    // 【新增】綠界物流相關欄位 (End)
    // ----------------------------------------------------------------
 // ----------------------------------------------------
    // 【新增】讓前端可以讀到 "userId": 1
    // ----------------------------------------------------
    @JsonProperty("userId")
    public Long getUserIdView() {
        return userid != null ? userid.getUserid() : null;
    }
}