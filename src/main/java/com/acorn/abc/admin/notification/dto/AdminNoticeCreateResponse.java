package com.acorn.abc.admin.notification.dto;

import com.acorn.abc.notification.domain.Notice;

import java.time.LocalDateTime;

// POST(등록) 전용 응답. notifyYn=true인 신규 등록에서 실제로 알림을 받은(트랜잭션 시점 JOINED/USER) 회원 수를
// notifyCreatedCount로 함께 반환한다 — 대상자가 0명이어도 정상이며 에러가 아니다.
public record AdminNoticeCreateResponse(
        Long noticeId,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int notifyCreatedCount
) {
    public static AdminNoticeCreateResponse from(Notice notice, int notifyCreatedCount) {
        return new AdminNoticeCreateResponse(notice.getNoticeId(), notice.getTitle(), notice.getContent(),
                notice.getNoticeStatus(), notice.getCreatedAt(), notice.getUpdatedAt(), notifyCreatedCount);
    }
}
