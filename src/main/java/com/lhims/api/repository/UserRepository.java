package com.lhims.api.repository;

import com.lhims.api.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByEmail(String email);

    List<UserAccount> findByIsActiveTrue();
}
