package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.NotificationService;
import com.lhims.api.web.dto.NotificationDtos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecuritySupport securitySupport;

    public NotificationController(NotificationService notificationService, SecuritySupport securitySupport) {
        this.notificationService = notificationService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<NotificationDtos.NotificationResponse> unread() {
        return notificationService.unreadFor(securitySupport.currentUser().userId());
    }

    @GetMapping("/all")
    public List<NotificationDtos.NotificationResponse> all() {
        return notificationService.allFor(securitySupport.currentUser().userId());
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markAsRead(id, securitySupport.currentUser().userId());
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount() {
        return notificationService.unreadCount(securitySupport.currentUser().userId());
    }
}
