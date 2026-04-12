package com.lhims.api.security;

import com.lhims.api.domain.enums.RoleCode;

public record CurrentUser(Long userId, String email, RoleCode role) {
}
