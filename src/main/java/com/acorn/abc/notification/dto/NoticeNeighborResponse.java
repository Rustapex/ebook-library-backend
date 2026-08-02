package com.acorn.abc.notification.dto;

import com.acorn.abc.notification.domain.Notice;

public record NoticeNeighborResponse(
        Long noticeId,
        String title
) {
    public static NoticeNeighborResponse from(Notice notice) {
        return new NoticeNeighborResponse(notice.getNoticeId(), notice.getTitle());
    }
}
