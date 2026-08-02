package com.acorn.abc.book.dto;

import java.time.LocalDate;
import java.util.List;

public record BookDetailResponse(
        Long bookId,
        String title,
        String isbn,
        String coverImageUrl,
        LocalDate pubDate,
        String publisherName,
        List<String> authors,
        List<BookCategoryResponse> categories,
        List<String> keywords,
        String description,
        String tableOfContents,
        String publisherReview,
        BookRentalInfoResponse rentalInfo,
        Integer totalPages,
        BookReviewSummaryResponse reviewSummary,
        String myRentalState,
        Boolean myFavoriteYn,
        boolean contentReady,
        boolean physicalLibrarySearchAvailable
) {
}
