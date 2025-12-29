package com.tw.shopping.main.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tw.shopping.main.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long>{
	
	
	// 賴 新增的 12/1
	// 【新增】後台會員搜尋與分頁
    // 邏輯：
    // 1. keyword: 同時比對 name, email, phone (只要其中一個符合就算)
    // 2. verified: 用來篩選 verifiedaccount (Boolean)
	// 當 :verified 為 false (未驗證) 時，我們同時接受 verifiedaccount = false 以及 verifiedaccount IS NULL
    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:keyword IS NULL OR (u.name LIKE CONCAT('%', CONCAT(:keyword, '%')) OR u.email LIKE CONCAT('%', CONCAT(:keyword, '%')) OR u.phone LIKE CONCAT('%', CONCAT(:keyword, '%')))) AND " +
           "(:verified IS NULL OR (u.verifiedAccount = :verified) OR (:verified = false AND u.verifiedAccount IS NULL))")
    Page<UserEntity> searchUsers(
            @Param("keyword") String keyword,
            @Param("verified") Boolean verified,
            Pageable pageable);
    
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"name", "phone", "birthday", "gender", "email", "address"}) 
	Optional<UserEntity> findById(Long id);
    Optional<UserEntity>findByIcon(byte[] icon);
    
	}

