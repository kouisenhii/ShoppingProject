package com.tw.shopping.main.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "userauthprovider",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_provider_identity",
                   columnNames = {"provider", "provideruserid"})
       }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------------------------------------------------------
    // 🔗 Many providers → One user
    // ----------------------------------------------------------
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userid", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private UserEntity user;

    // ----------------------------------------------------------
    // Provider 資料欄位
    // ----------------------------------------------------------
    @Column(nullable = false, length = 20)
    private String provider;  // GOOGLE / LINE / LOCAL / FACEBOOK

    @Column(name = "provideruserid", nullable = false, length = 200)
    private String providerUserid;  // Google sub / LINE userId

    @Column(name = "provideremail", length = 255)
    private String providerEmail;

    @Column(name = "providername", length = 255)
    private String providerName;

    @Column(name = "providerpicture", length = 500)
    private String providerPicture;

    @Column(name = "createdat", nullable = false)
    private LocalDateTime createdAt;

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
}
