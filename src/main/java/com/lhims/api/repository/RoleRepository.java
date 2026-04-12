package com.lhims.api.repository;

import com.lhims.api.domain.entity.Role;
import com.lhims.api.domain.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
