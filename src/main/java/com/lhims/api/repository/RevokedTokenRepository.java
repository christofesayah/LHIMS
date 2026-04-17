package com.lhims.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByToken(String token);
}
