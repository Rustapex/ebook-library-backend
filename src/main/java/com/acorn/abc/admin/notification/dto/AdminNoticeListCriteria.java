package com.acorn.abc.admin.notification.dto;

public record AdminNoticeListCriteria(
        String status,
        int page,
        int size
) {
}
