package com.lhims.api.service;

import com.lhims.api.domain.entity.AuditLog;
import com.lhims.api.domain.entity.Region;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.repository.AuditLogRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.web.dto.AuditDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository,
                        RegionRepository regionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
    }

    @Transactional
    public void log(Long actorUserId, Long targetUserId, AuditActionType actionType, String entityName,
                    String entityId, String oldValue, String newValue, HttpServletRequest request) {
        log(actorUserId, targetUserId, null, actionType, entityName, entityId, oldValue, newValue, request);
    }

    @Transactional
    public void log(Long actorUserId, Long targetUserId, Long regionId, AuditActionType actionType, String entityName,
                    String entityId, String oldValue, String newValue, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        UserAccount actor = actorUserId == null ? null : userRepository.findById(actorUserId).orElse(null);
        UserAccount target = targetUserId == null ? null : userRepository.findById(targetUserId).orElse(null);
        Region region = regionId == null ? null : regionRepository.findById(regionId).orElse(null);
        log.setActorUser(actor);
        log.setTargetUser(target);
        log.setRegion(region);
        log.setActionType(actionType);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(request == null ? null : request.getRemoteAddr());
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditDtos.AuditLogResponse> allLogs(Long userId, Long regionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs;
        if (userId != null) {
            logs = auditLogRepository.findByActorUserUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (regionId != null) {
            logs = auditLogRepository.findByRegionRegionIdOrderByCreatedAtDesc(regionId, pageable);
        } else {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return logs.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditDtos.AuditLogResponse> myLogs(Long userId, int page, int size) {
        return auditLogRepository.findByActorUserUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toDto);
    }

    private AuditDtos.AuditLogResponse toDto(AuditLog log) {
        return new AuditDtos.AuditLogResponse(
                log.getAuditId(),
                log.getActorUser() == null ? null : log.getActorUser().getUserId(),
                log.getTargetUser() == null ? null : log.getTargetUser().getUserId(),
                log.getRegion() == null ? null : log.getRegion().getRegionId(),
                log.getActionType(),
                log.getEntityName(),
                log.getEntityId(),
                log.getOldValue(),
                log.getNewValue(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
