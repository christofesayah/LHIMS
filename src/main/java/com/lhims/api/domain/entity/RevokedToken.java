package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revoked_token_id")
    private Long revokedTokenId;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "text")
    private String token;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    public Long getRevokedTokenId() {
        return revokedTokenId;
    }

    public void setRevokedTokenId(Long revokedTokenId) {
        this.revokedTokenId = revokedTokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
