package com.lhims.api.service;

import com.lhims.api.exception.NotFoundException;
import com.lhims.api.repository.NotificationRepository;
import com.lhims.api.web.dto.NotificationDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> unreadFor(Long userId) {
        return notificationRepository.findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> allFor(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCountResponse unreadCount(Long userId) {
        return new NotificationDtos.UnreadCountResponse(notificationRepository.countByUserUserIdAndIsReadFalse(userId));
    }

    private NotificationDtos.NotificationResponse toDto(com.lhims.api.domain.entity.Notification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getNotificationId(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
