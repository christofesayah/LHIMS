package com.lhims.api.web.dto;

import java.time.LocalDateTime;

import com.lhims.api.domain.enums.RoleCode;

public class UserDtos {

    public record UpdateMyProfileRequest(
            String firstName,
            String lastName,
            String username,
            String email
    ) {
    }

    public record UpdateUserRoleRequest(RoleCode role) {
    }

    public record UpdateUserStatusRequest(boolean active) {
    }

    public record UserProfile(
            Long userId,
            String username,
            String firstName,
            String lastName,
            String email,
            Boolean isActive,
            RoleCode role,
            LocalDateTime createdAt,
            Long assignedFacilityId,
            Boolean isApproved
    ) {}
}