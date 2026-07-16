package com.acorn.abc.admin.collection.dto;

import java.time.LocalDateTime;

public record AdminCollectionSaveResponse(
        Long collectionId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
