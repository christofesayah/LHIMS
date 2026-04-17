package com.lhims.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.Role;
import com.lhims.api.domain.enums.RoleCode;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
