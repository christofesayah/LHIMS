package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password, @NotNull RoleCode role) {
    }

    public record LoginResponse(String token, RoleCode role, Long userId) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull RoleCode role,
            Long facilityId
    ) {
    }

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }
}
