package com.tw.shopping.main.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "userinfo") // 對應 ER 圖中的表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid") // 對應 ER 圖的主鍵
    private Long userid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String address;

    private String phone;

    private String gender;

    private Date birthday; // ER 圖是 datetime，也可以用 LocalDate

    // ER 圖中 icon 是 mediumblob，對應 Java 的 byte[]，並加上 @Lob
    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] icon;

    @Column(name = "verifiedaccount")
    private Boolean verifiedAccount; // bit(1) 對應 Boolean

    @CreationTimestamp
    @Column(name = "createdat", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedat", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // --------------------------------------------------------------
    // 🔗 一個 user 可以綁多個 provider
    // --------------------------------------------------------------
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private List<UserAuthProviderEntity> providers;
    
    // ----------------------------------------------------------------
    // 關聯設定 (JoinColumn & 無窮迴圈預防)
    // ----------------------------------------------------------------

    // 1. 與購物車的關聯 (一對多)
    // mappedBy = "user" 對應 CartEntity 中的 private UserEntity user;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore // 【關鍵】防止 JSON 序列化時進入無窮迴圈
    @ToString.Exclude
    private List<CartEntity> cartItems;

    // 2. 與訂單的關聯 (一對多)
    // mappedBy = "userInfo" 對應 OrderEntity 中的欄位名稱 (稍後在 OrderEntity 設定)
    @OneToMany(mappedBy = "userid", fetch = FetchType.LAZY)
    @JsonIgnore // 【關鍵】防止 JSON 序列化時進入無窮迴圈
    @ToString.Exclude
    private List<OrderEntity> orders;
    
    // 賴 新增的 12/6
    /**
     * 防禦措施說明：
     * 1. FetchType.EAGER: 登入驗證時需要立即讀取權限，避免 "Session closed" 錯誤。
     * 2. @JoinTable: 指定全小寫的關聯表 userrole。
     * 3. @EqualsAndHashCode.Exclude & @ToString.Exclude: 
     * 防止 Lombok 在生成 hashCode 或 toString 時去讀取 lazy loading 的資料，
     * 或者在雙向關聯時造成無窮迴圈。
     */
    @ManyToMany(fetch = FetchType.EAGER) 
    @JoinTable(
        name = "userrole",                  // 關聯表名 (全小寫)
        joinColumns = @JoinColumn(name = "userid"), // 本表 ID (全小寫)
        inverseJoinColumns = @JoinColumn(name = "roleid") // 對方 ID (全小寫)
    )
    @ToString.Exclude           //  防禦 Lombok toString 迴圈
    @EqualsAndHashCode.Exclude  //  防禦 Set 集合操作時的 hashCode 迴圈
    private Set<RoleEntity> roles = new HashSet<>();
}