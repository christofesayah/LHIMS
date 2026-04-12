package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.AuditService;
import com.lhims.api.web.dto.AuditDtos;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/logs")
public class AuditController {

    private final AuditService auditService;
    private final SecuritySupport securitySupport;

    public AuditController(AuditService auditService, SecuritySupport securitySupport) {
        this.auditService = auditService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public Page<AuditDtos.AuditLogResponse> all(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Long regionId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return auditService.allLogs(userId, regionId, page, size);
    }

    @GetMapping("/me")
    public Page<AuditDtos.AuditLogResponse> me(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return auditService.myLogs(securitySupport.currentUser().userId(), page, size);
    }
}
