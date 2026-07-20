package com.acorn.abc.admin.book.dto;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookDetail;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 도서 상세(API-ADMIN-BOOK-005) 응답. 현재 연결된 작가·카테고리는 비활성이어도 노출하되 status를
// 함께 반환해 화면이 구분할 수 있게 한다(관리자 도서 API PR52 코드리뷰 대응계획 §2.1). BookPage/detailContent/
// 키워드/파일/누적 집계값은 이 응답에 포함하지 않는다.
public record AdminBookDetailResponse(
        Long bookId,
        String title,
        String isbn,
        String publisherName,
        List<AuthorItem> authors,
        List<CategoryItem> categories,
        String rentalType,
        int rentalPrice,
        int defaultRentalDays,
        String coverImageUrl,
        DetailInfo detail,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record AuthorItem(Long authorId, String authorName, String status) {
    }

    public record CategoryItem(Long categoryId, String categoryName, String status) {
    }

    public record DetailInfo(String description, String tableOfContents, String publisherReview) {
    }

    public static AdminBookDetailResponse of(Book book, List<AuthorItem> authors, List<CategoryItem> categories,
                                              BookDetail bookDetail) {
        DetailInfo detailInfo = bookDetail == null
                ? new DetailInfo(null, null, null)
                : new DetailInfo(bookDetail.getDescription(), bookDetail.getTableOfContents(), bookDetail.getPublisherReview());
        return new AdminBookDetailResponse(
                book.getBookId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublisher() == null ? null : book.getPublisher().getPublisherName(),
                authors,
                categories,
                book.getRentalType(),
                book.getRentalPrice(),
                book.getDefaultRentalDays(),
                book.getCoverImageUrl(),
                detailInfo,
                book.getStatus(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
