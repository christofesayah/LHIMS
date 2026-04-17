package com.lhims.api.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lhims.api.domain.entity.Role;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.entity.UserRole;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.domain.enums.RoleCode;
import com.lhims.api.exception.BadRequestException;
import com.lhims.api.repository.RevokedTokenRepository;
import com.lhims.api.repository.RoleRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.repository.UserRoleRepository;
import com.lhims.api.security.JwtUtil;
import com.lhims.api.web.dto.AuthDtos;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                passwordEncoder,
                jwtUtil,
                userRepository,
                roleRepository,
                userRoleRepository,
                revokedTokenRepository,
                auditService
        );
    }

    @Test
    void loginReturnsTokenWhenRoleMatches() {
        UserAccount user = new UserAccount();
        user.setUserId(1L);
        user.setEmail("ministry@lhims.com");

        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getCode()).thenReturn(RoleCode.MINISTRY_OFFICIAL);

        UserRole userRole = new UserRole();
        userRole.setRole(role);

        when(userRepository.findByEmail("ministry@lhims.com")).thenReturn(Optional.of(user));
        when(userRoleRepository.findTopByUserUserIdOrderByAssignedAtDesc(1L)).thenReturn(Optional.of(userRole));
        when(jwtUtil.generateToken(1L, "ministry@lhims.com", RoleCode.MINISTRY_OFFICIAL)).thenReturn("jwt-token");

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("ministry@lhims.com", "secret", RoleCode.MINISTRY_OFFICIAL),
                httpServletRequest
        );

        assertEquals("jwt-token", response.token());
        assertEquals(RoleCode.MINISTRY_OFFICIAL, response.role());
        assertEquals(1L, response.userId());
        verify(authenticationManager).authenticate(any());
        verify(auditService).log(
                eq(1L),
                isNull(),
                eq(AuditActionType.LOGIN),
                eq("users"),
                eq("1"),
                isNull(),
                eq("LOGIN"),
                eq(httpServletRequest)
        );
    }

    @Test
    void loginThrowsWhenRoleDoesNotMatch() {
        UserAccount user = new UserAccount();
        user.setUserId(1L);
        user.setEmail("admin@lhims.com");

        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getCode()).thenReturn(RoleCode.HOSPITAL_ADMIN);

        UserRole userRole = new UserRole();
        userRole.setRole(role);

        when(userRepository.findByEmail("admin@lhims.com")).thenReturn(Optional.of(user));
        when(userRoleRepository.findTopByUserUserIdOrderByAssignedAtDesc(1L)).thenReturn(Optional.of(userRole));

        assertThrows(BadRequestException.class, () -> authService.login(
                new AuthDtos.LoginRequest("admin@lhims.com", "secret", RoleCode.PUBLIC),
                httpServletRequest
        ));
    }
}
