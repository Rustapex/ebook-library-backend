package com.acorn.abc.admin.collection.dto;

import com.acorn.abc.collection.domain.CollectionBook;

public record AdminCollectionBookResponse(
        Long collectionBookId,
        Long bookId,
        String title,
        String status,
        Integer displayOrder
) {
    public static AdminCollectionBookResponse from(CollectionBook collectionBook) {
        return new AdminCollectionBookResponse(
                collectionBook.getCollectionBookId(),
                collectionBook.getBook().getBookId(),
                collectionBook.getBook().getTitle(),
                collectionBook.getBook().getStatus(),
                collectionBook.getDisplayOrder());
    }
}
