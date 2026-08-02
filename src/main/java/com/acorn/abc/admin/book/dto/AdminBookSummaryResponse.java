package com.acorn.abc.admin.book.dto;

import com.acorn.abc.book.domain.Book;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 도서 목록(API-ADMIN-BOOK-001) 항목. 공개 목록과 달리 HIDDEN/INACTIVE도 그대로 노출한다.
public record AdminBookSummaryResponse(
        Long bookId,
        String title,
        String isbn,
        String publisherName,
        List<String> authorNames,
        List<CategoryItem> categories,
        String rentalType,
        int rentalPrice,
        int defaultRentalDays,
        String coverImageUrl,
        String status,
        LocalDateTime createdAt
) {
    public record CategoryItem(Long categoryId, String categoryName) {
    }

    public static AdminBookSummaryResponse of(Book book, List<String> authorNames, List<CategoryItem> categories) {
        return new AdminBookSummaryResponse(
                book.getBookId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublisher() == null ? null : book.getPublisher().getPublisherName(),
                authorNames,
                categories,
                book.getRentalType(),
                book.getRentalPrice(),
                book.getDefaultRentalDays(),
                book.getCoverImageUrl(),
                book.getStatus(),
                book.getCreatedAt());
    }
}
