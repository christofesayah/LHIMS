package com.lhims.api.web.dto;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(Long notificationId, String message, Boolean isRead, LocalDateTime createdAt) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
