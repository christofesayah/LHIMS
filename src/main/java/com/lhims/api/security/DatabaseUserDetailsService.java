package com.lhims.api.security;

import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.entity.UserRole;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DatabaseUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserRole latestRole = userRoleRepository.findTopByUserUserIdOrderByAssignedAtDesc(user.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Role not found"));
        String authority = "ROLE_" + latestRole.getRole().getCode().name();
        return new AuthenticatedUser(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getIsActive()),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
