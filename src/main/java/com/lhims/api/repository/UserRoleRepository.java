package com.lhims.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findTopByUserUserIdOrderByAssignedAtDesc(Long userId);

    List<UserRole> findByUserUserId(Long userId);
}
