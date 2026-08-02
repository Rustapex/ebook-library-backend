package com.acorn.abc.notification.dto;

public record NotificationListCriteria(
        Boolean readYn,
        String notificationType,
        int page,
        int size
) {
}
