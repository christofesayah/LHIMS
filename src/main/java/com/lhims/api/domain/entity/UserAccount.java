package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "createdByUser")
    private List<SimulationScenario> createdScenarios = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(mappedBy = "assignedByUser")
    private List<UserRole> assignedRoles = new ArrayList<>();

    @OneToMany(mappedBy = "actorUser")
    private List<AuditLog> performedAuditLogs = new ArrayList<>();

    @OneToMany(mappedBy = "targetUser")
    private List<AuditLog> affectedAuditLogs = new ArrayList<>();

    protected UserAccount() {
    }
}
