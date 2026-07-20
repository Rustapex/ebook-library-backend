package com.acorn.abc.notification.dto;

import com.acorn.abc.notification.domain.Notice;

import java.time.LocalDateTime;

public record NoticeItemResponse(
        Long noticeId,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static NoticeItemResponse from(Notice notice) {
        return new NoticeItemResponse(notice.getNoticeId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }
}
