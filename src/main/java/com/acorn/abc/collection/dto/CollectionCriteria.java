package com.acorn.abc.collection.dto;

public record CollectionCriteria(
        String collectionType,
        int page,
        int size,
        Integer previewSize
) {
}
