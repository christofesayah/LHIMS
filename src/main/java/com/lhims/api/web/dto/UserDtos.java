package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserProfile(
            Long userId,
            String username,
            String email,
            Boolean active,
            RoleCode role,
            LocalDateTime createdAt,
            Long assignedFacilityId,
            Boolean isApproved
    ) {
    }

    public record UpdateMyProfileRequest(@NotBlank String username, @Email @NotBlank String email) {
    }

    public record UpdateUserRoleRequest(@NotNull RoleCode role) {
    }

    public record UpdateUserStatusRequest(@NotNull Boolean active) {
    }
}
