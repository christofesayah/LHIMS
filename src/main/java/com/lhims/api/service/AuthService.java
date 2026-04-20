package com.lhims.api.service;

import com.lhims.api.domain.entity.RevokedToken;
import com.lhims.api.domain.entity.Role;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.entity.UserRole;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.exception.BadRequestException;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.repository.HealthFacilityRepository;
import com.lhims.api.repository.RevokedTokenRepository;
import com.lhims.api.repository.RoleRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.repository.UserRoleRepository;
import com.lhims.api.security.JwtUtil;
import com.lhims.api.web.dto.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final AuditService auditService;
    private final HealthFacilityRepository healthFacilityRepository;

    public AuthService(AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                       RevokedTokenRepository revokedTokenRepository, AuditService auditService,
                       HealthFacilityRepository healthFacilityRepository) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.auditService = auditService;
        this.healthFacilityRepository = healthFacilityRepository;
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, HttpServletRequest httpServletRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserAccount user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User not found"));
        UserRole userRole = userRoleRepository.findTopByUserUserIdOrderByAssignedAtDesc(user.getUserId())
                .orElseThrow(() -> new NotFoundException("Role not assigned"));
        if (!userRole.getRole().getCode().equals(request.role())) {
            throw new BadRequestException("Selected role does not match your assigned role");
        }
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), userRole.getRole().getCode());
        auditService.log(user.getUserId(), null, AuditActionType.LOGIN, "users", String.valueOf(user.getUserId()),
                null, "LOGIN", httpServletRequest);
        return new AuthDtos.LoginResponse(token, userRole.getRole().getCode(), user.getUserId());
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest request, Long actorUserId, HttpServletRequest httpServletRequest) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsActive(true);
        // If registered by system (null actor), it's a request, so not approved yet.
        // If registered by Ministry Official, it's auto-approved.
        user.setIsApproved(actorUserId != null);
        user.setCreatedAt(LocalDateTime.now());

        if (request.facilityId() != null) {
            user.setAssignedFacility(healthFacilityRepository.findById(request.facilityId())
                    .orElseThrow(() -> new NotFoundException("Facility not found")));
        }

        UserAccount saved = userRepository.save(user);

        Role role = roleRepository.findByCode(request.role())
                .orElseThrow(() -> new BadRequestException("Role not found: " + request.role()));
        UserRole userRole = new UserRole();
        userRole.setUser(saved);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());
        if (actorUserId != null) {
            userRepository.findById(actorUserId).ifPresent(userRole::setAssignedByUser);
        }
        userRoleRepository.save(userRole);

        auditService.log(actorUserId, saved.getUserId(), AuditActionType.CREATE, "users", String.valueOf(saved.getUserId()),
                null, "REGISTER", httpServletRequest);
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest request, HttpServletRequest httpServletRequest) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.log(userId, userId, AuditActionType.UPDATE, "users", String.valueOf(userId),
                "PASSWORD_CHANGE", "PASSWORD_CHANGE", httpServletRequest);
    }

    @Transactional
    public void logout(String token) {
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(token);
        revokedToken.setRevokedAt(LocalDateTime.now());
        revokedTokenRepository.save(revokedToken);
    }
}
