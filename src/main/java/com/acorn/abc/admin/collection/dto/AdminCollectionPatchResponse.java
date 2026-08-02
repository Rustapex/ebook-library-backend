package com.acorn.abc.admin.collection.dto;

public record AdminCollectionPatchResponse(
        Long collectionId,
        String status,
        long mappedCount
) {
}
