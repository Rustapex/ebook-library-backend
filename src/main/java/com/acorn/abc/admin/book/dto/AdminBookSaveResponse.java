package com.acorn.abc.admin.book.dto;

import java.time.LocalDateTime;

// 등록(002)·수정(003) 공통 응답. AdminCollectionSaveResponse와 동일한 기존 관례로 두 API가 같은 타입을 쓴다.
public record AdminBookSaveResponse(
        Long bookId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
