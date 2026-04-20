package com.lhims.api.service;

import com.lhims.api.domain.entity.Role;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.entity.UserRole;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.exception.BadRequestException;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.repository.RoleRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.repository.UserRoleRepository;
import com.lhims.api.web.dto.UserDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                       RoleRepository roleRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserDtos.UserProfile me(Long userId) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return toProfile(user);
    }

    @Transactional
    public UserDtos.UserProfile updateMe(Long userId, UserDtos.UpdateMyProfileRequest request) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setUsername(request.username());
        user.setEmail(request.email());
        return toProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserProfile> listAllUsers() {
        return userRepository.findAll().stream().map(this::toProfile).toList();
    }

    @Transactional
    public void changeRole(Long userId, UserDtos.UpdateUserRoleRequest request, Long actorUserId, HttpServletRequest httpServletRequest) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Role role = roleRepository.findByCode(request.role())
                .orElseThrow(() -> new BadRequestException("Role not found"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());
        if (actorUserId != null) {
            userRepository.findById(actorUserId).ifPresent(userRole::setAssignedByUser);
        }
        userRoleRepository.save(userRole);

        auditService.log(actorUserId, userId, AuditActionType.ASSIGN_ROLE, "users", String.valueOf(userId),
                null, request.role().name(), httpServletRequest);
    }

    @Transactional
    public void updateStatus(Long userId, UserDtos.UpdateUserStatusRequest request, Long actorUserId, HttpServletRequest httpServletRequest) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setIsActive(request.active());
        userRepository.save(user);
        auditService.log(actorUserId, userId, AuditActionType.UPDATE, "users", String.valueOf(userId),
                null, "status=" + request.active(), httpServletRequest);
    }

    @Transactional
    public void deleteUser(Long userId, Long actorUserId, HttpServletRequest httpServletRequest) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        auditService.log(actorUserId, userId, AuditActionType.DELETE, "users", String.valueOf(userId),
                null, "DEACTIVATE", httpServletRequest);
    }

    private UserDtos.UserProfile toProfile(UserAccount user) {
        var role = userRoleRepository.findTopByUserUserIdOrderByAssignedAtDesc(user.getUserId())
                .map(UserRole::getRole)
                .map(Role::getCode)
                .orElse(null);
        return new UserDtos.UserProfile(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsActive(),
                role,
                user.getCreatedAt(),
                user.getAssignedFacility() != null ? user.getAssignedFacility().getFacilityId() : null,
                user.getIsApproved()
        );
    }
}
