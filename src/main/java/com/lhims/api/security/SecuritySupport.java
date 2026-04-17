package com.lhims.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.lhims.api.domain.enums.RoleCode;

@Component
public class SecuritySupport {

    public CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            throw new IllegalStateException("No authenticated user");
        }

        if (auth.getPrincipal() instanceof AuthenticatedUser au) {
            String authority = au.getAuthorities().stream().findFirst().orElseThrow().getAuthority();
            RoleCode role = RoleCode.valueOf(authority.replace("ROLE_", ""));
            return new CurrentUser(au.getUserId(), au.getUsername(), role);
        }

        String authority = userDetails.getAuthorities().stream().findFirst().orElseThrow().getAuthority();
        RoleCode role = RoleCode.valueOf(authority.replace("ROLE_", ""));
        return new CurrentUser(null, userDetails.getUsername(), role);
    }
}
