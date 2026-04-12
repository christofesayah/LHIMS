package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.AuditActionType;

import java.time.LocalDateTime;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(Long auditId, Long actorUserId, Long targetUserId, Long regionId, AuditActionType actionType,
                                   String entityName, String entityId, String oldValue, String newValue,
                                   String ipAddress, String userAgent, LocalDateTime createdAt) {
    }
}
