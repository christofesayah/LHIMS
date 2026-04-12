package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.UserService;
import com.lhims.api.web.dto.UserDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SecuritySupport securitySupport;

    public UserController(UserService userService, SecuritySupport securitySupport) {
        this.userService = userService;
        this.securitySupport = securitySupport;
    }

    @GetMapping("/me")
    public UserDtos.UserProfile me() {
        return userService.me(securitySupport.currentUser().userId());
    }

    @PutMapping("/me")
    public UserDtos.UserProfile updateMe(@Valid @RequestBody UserDtos.UpdateMyProfileRequest request) {
        return userService.updateMe(securitySupport.currentUser().userId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public List<UserDtos.UserProfile> all() {
        return userService.listAllUsers();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public void changeRole(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRoleRequest request,
                           HttpServletRequest httpServletRequest) {
        userService.changeRole(id, request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public void status(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserStatusRequest request,
                       HttpServletRequest httpServletRequest) {
        userService.updateStatus(id, request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public void delete(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        userService.deleteUser(id, securitySupport.currentUser().userId(), httpServletRequest);
    }
}
