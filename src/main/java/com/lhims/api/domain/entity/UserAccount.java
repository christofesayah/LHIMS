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

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "facility_id")
    private HealthFacility assignedFacility;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean isApproved) {
        this.isApproved = isApproved;
    }

    public HealthFacility getAssignedFacility() {
        return assignedFacility;
    }

    public void setAssignedFacility(HealthFacility assignedFacility) {
        this.assignedFacility = assignedFacility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SimulationScenario> getCreatedScenarios() {
        return createdScenarios;
    }

    public void setCreatedScenarios(List<SimulationScenario> createdScenarios) {
        this.createdScenarios = createdScenarios;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public List<UserRole> getAssignedRoles() {
        return assignedRoles;
    }

    public void setAssignedRoles(List<UserRole> assignedRoles) {
        this.assignedRoles = assignedRoles;
    }

    public List<AuditLog> getPerformedAuditLogs() {
        return performedAuditLogs;
    }

    public void setPerformedAuditLogs(List<AuditLog> performedAuditLogs) {
        this.performedAuditLogs = performedAuditLogs;
    }

    public List<AuditLog> getAffectedAuditLogs() {
        return affectedAuditLogs;
    }

    public void setAffectedAuditLogs(List<AuditLog> affectedAuditLogs) {
        this.affectedAuditLogs = affectedAuditLogs;
    }

    public UserAccount() {
    }
}
