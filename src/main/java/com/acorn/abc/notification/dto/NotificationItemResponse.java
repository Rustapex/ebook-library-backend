package com.acorn.abc.notification.dto;

import com.acorn.abc.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationItemResponse(
        Long notificationId,
        String notificationType,
        String title,
        String targetType,
        Long targetId,
        Boolean readYn,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationItemResponse from(Notification notification) {
        return new NotificationItemResponse(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getReadYn(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
