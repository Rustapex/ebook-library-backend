package com.acorn.abc.notification.dto;

import com.acorn.abc.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationReadResponse(
        Long notificationId,
        Boolean readYn,
        LocalDateTime readAt,
        String targetType,
        Long targetId
) {
    public static NotificationReadResponse from(Notification notification) {
        return new NotificationReadResponse(
                notification.getNotificationId(),
                notification.getReadYn(),
                notification.getReadAt(),
                notification.getTargetType(),
                notification.getTargetId()
        );
    }
}
