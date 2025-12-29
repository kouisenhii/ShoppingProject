package com.tw.shopping.main.repository;

import com.tw.shopping.main.entity.RoleEntity;
import com.tw.shopping.main.entity.RoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {
    // 透過角色名稱尋找 (例如找 "ROLE_ADMIN")
    Optional<RoleEntity> findByRoleName(String roleName);
}