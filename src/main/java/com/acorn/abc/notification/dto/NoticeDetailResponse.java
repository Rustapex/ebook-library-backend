package com.acorn.abc.notification.dto;

import java.time.LocalDateTime;

// prevNotice/nextNotice는 값이 없어도 키 자체는 항상 JSON에 포함되어야 하므로(NoticeItemResponse와 달리)
// @JsonInclude(NON_NULL)을 붙이지 않는다 — 프로젝트 전역 Jackson 기본값(ALWAYS)을 그대로 따른다.
public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        LocalDateTime createdAt,
        NoticeNeighborResponse prevNotice,
        NoticeNeighborResponse nextNotice
) {
}
