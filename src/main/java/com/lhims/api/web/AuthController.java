package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.AuthService;
import com.lhims.api.web.dto.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecuritySupport securitySupport;

    public AuthController(AuthService authService, SecuritySupport securitySupport) {
        this.authService = authService;
        this.securitySupport = securitySupport;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.login(request, httpServletRequest));
    }

    @PostMapping("/request-account")
    public ResponseEntity<AuthDtos.LoginResponse> requestAccount(@Valid @RequestBody AuthDtos.RegisterRequest request,
                                               HttpServletRequest httpServletRequest) {
        AuthDtos.LoginResponse response = authService.register(request, null, httpServletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public ResponseEntity<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request,
                                         HttpServletRequest httpServletRequest) {
        Long actorUserId = securitySupport.currentUser().userId();
        AuthDtos.LoginResponse response = authService.register(request, actorUserId, httpServletRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request,
                                               HttpServletRequest httpServletRequest) {
        Long userId = securitySupport.currentUser().userId();
        authService.changePassword(userId, request, httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return ResponseEntity.ok().build();
    }
}
