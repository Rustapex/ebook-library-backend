package com.acorn.abc.admin.notification.dto;

import com.acorn.abc.notification.domain.Notice;

import java.time.LocalDateTime;

// 목록 조회 및 PUT(수정) 응답에 사용. POST(등록) 전용 notifyCreatedCount는 여기에 없다 —
// 수정 시엔 알림을 재발송하지 않으므로 이 필드 자체가 개념적으로 무의미하기 때문.
public record AdminNoticeResponse(
        Long noticeId,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(notice.getNoticeId(), notice.getTitle(), notice.getContent(),
                notice.getNoticeStatus(), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
