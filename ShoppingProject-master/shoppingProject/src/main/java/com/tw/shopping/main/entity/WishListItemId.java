package com.tw.shopping.main.entity;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data // 提供 Getter, Setter, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class WishListItemId implements Serializable {

    // 複合主鍵的第一個欄位
    @Column(name = "userid", nullable = false)
    private Long userId;

    // 複合主鍵的第二個欄位
    @Column(name = "productid", nullable = false)
    private Integer productId;
    
    // ⭐ 注意：由於使用了 @Data，Lombok 會自動生成 equals() 和 hashCode()
    // 這是複合主鍵的必要條件，以確保物件比對的正確性。
}