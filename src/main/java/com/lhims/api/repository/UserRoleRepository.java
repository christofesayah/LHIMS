package com.lhims.api.repository;

import com.lhims.api.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findTopByUserUserIdOrderByAssignedAtDesc(Long userId);

    List<UserRole> findByUserUserId(Long userId);
}
