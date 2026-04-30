package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.RoleCode;

public class AuthDtos {
    public record LoginRequest(String email, String password, RoleCode role) {
    }

    public record LoginResponse(String token, RoleCode role, Long userId) {
    }

    public record RegisterRequest(
            String firstName,
            String lastName,
            String username,
            String email,
            String password,
            RoleCode role,
            Long facilityId
    ) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
}