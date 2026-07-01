package com.acorn.abc.book.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookAuthor;
import com.acorn.abc.book.domain.BookDetail;
import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.dto.BookCategoryResponse;
import com.acorn.abc.book.dto.BookDetailResponse;
import com.acorn.abc.book.dto.BookListCriteria;
import com.acorn.abc.book.dto.BookRecommendationResponse;
import com.acorn.abc.book.dto.BookRentalInfoResponse;
import com.acorn.abc.book.dto.BookReviewSummaryResponse;
import com.acorn.abc.book.dto.BookSearchCriteria;
import com.acorn.abc.book.dto.BookSearchResponse;
import com.acorn.abc.book.repository.BookAuthorRepository;
import com.acorn.abc.book.repository.BookCategoryRepository;
import com.acorn.abc.book.repository.BookDetailRepository;
import com.acorn.abc.book.repository.BookKeywordMapRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.repository.BookSpecifications;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final String AVAILABLE = "AVAILABLE";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    private static final int MAX_RECOMMENDATION_LIMIT = 50;

    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookKeywordMapRepository bookKeywordMapRepository;
    private final BookDetailRepository bookDetailRepository;

    public PageResponse<BookCardResponse> getBooks(
            Long categoryId,
            Long parentCategoryId,
            Long collectionId,
            String rentalType,
            String section,
            String sort,
            Integer page,
            Integer size
    ) {
        BookListCriteria criteria = new BookListCriteria(
                categoryId,
                parentCategoryId,
                collectionId,
                normalizeRentalType(rentalType),
                normalizeText(section),
                normalizeSort(sort),
                normalizePage(page),
                normalizeSize(size)
        );

        Specification<Book> spec = BookSpecifications.available()
                .and(BookSpecifications.hasCategory(criteria.categoryId()))
                .and(BookSpecifications.hasParentCategory(criteria.parentCategoryId()))
                .and(BookSpecifications.inCollection(criteria.collectionId()))
                .and(BookSpecifications.inEventSection("event".equalsIgnoreCase(criteria.section())))
                .and(BookSpecifications.hasRentalType(criteria.rentalType()));

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sortFor(criteria.sort()));
        Page<Book> books = bookRepository.findAll(spec, pageable);
        return toPageResponse(books);
    }

    public BookSearchResponse searchBooks(String keyword, Long categoryId, String rentalType, String sort, Integer page, Integer size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String trimmedKeyword = keyword.trim();
        BookSearchCriteria criteria = new BookSearchCriteria(
                trimmedKeyword,
                categoryId,
                normalizeRentalType(rentalType),
                normalizeSort(sort),
                normalizePage(page),
                normalizeSize(size)
        );

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sortFor(criteria.sort()));
        Page<Book> result = bookRepository.search(
                "%" + criteria.keyword() + "%",
                criteria.categoryId(),
                criteria.rentalType(),
                pageable
        );
        PageResponse<BookCardResponse> resultPage = toPageResponse(result);
        return new BookSearchResponse(trimmedKeyword, resultPage.totalElements(), resultPage);
    }

    public BookDetailResponse getBookDetail(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(candidate -> AVAILABLE.equals(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        BookDetail detail = bookDetailRepository.findByBook_BookId(bookId).orElse(null);
        List<String> authors = bookAuthorRepository.findActiveByBookId(bookId).stream()
                .map(bookAuthor -> bookAuthor.getAuthor().getAuthorName())
                .toList();
        List<BookCategoryResponse> categories = bookCategoryRepository.findActiveByBookId(bookId).stream()
                .map(bookCategory -> new BookCategoryResponse(
                        bookCategory.getCategory().getCategoryId(),
                        bookCategory.getCategory().getParent() == null ? null : bookCategory.getCategory().getParent().getCategoryId(),
                        bookCategory.getCategory().getCategoryName(),
                        bookCategory.getCategory().getDisplayOrder()
                ))
                .toList();
        List<String> keywords = bookKeywordMapRepository.findActiveByBookId(bookId).stream()
                .map(bookKeywordMap -> bookKeywordMap.getKeyword().getKeywordName())
                .toList();

        BookRentalInfoResponse rentalInfo = new BookRentalInfoResponse(
                book.getRentalType(),
                book.getRentalPrice(),
                book.getDefaultRentalDays()
        );
        BookReviewSummaryResponse reviewSummary = new BookReviewSummaryResponse(
                defaultBigDecimal(book.getAverageRating()),
                book.getReviewCount()
        );

        return new BookDetailResponse(
                book.getBookId(),
                book.getTitle(),
                book.getIsbn(),
                book.getCoverImageUrl(),
                book.getPubDate(),
                book.getPublisher() == null ? null : book.getPublisher().getPublisherName(),
                authors,
                categories,
                keywords,
                detail == null ? null : detail.getDescription(),
                detail == null ? null : detail.getTableOfContents(),
                detail == null ? null : detail.getPublisherReview(),
                rentalInfo,
                book.getTotalPages(),
                reviewSummary,
                null,
                false
        );
    }

    public BookRecommendationResponse getRecommendations(Long baseBookId, String type, Integer limit) {
        String recommendationType = normalizeRecommendationType(type, baseBookId);
        int normalizedLimit = normalizeRecommendationLimit(limit);
        Pageable pageable = PageRequest.of(0, normalizedLimit);
        List<Book> books = bookRepository.findRecommendations(baseBookId, recommendationType, pageable);
        return new BookRecommendationResponse(recommendationType, toCardResponses(books));
    }

    public List<BookCardResponse> findCardsByCollectionId(Long collectionId, Integer limit) {
        Pageable pageable = limit == null ? Pageable.unpaged() : PageRequest.of(0, limit);
        List<Book> books = bookRepository.findByCollectionId(collectionId, pageable);
        return toCardResponses(books);
    }

    private PageResponse<BookCardResponse> toPageResponse(Page<Book> page) {
        List<BookCardResponse> content = toCardResponses(page.getContent());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private List<BookCardResponse> toCardResponses(List<Book> books) {
        if (books.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = books.stream().map(Book::getBookId).toList();
        Map<Long, List<String>> authorsByBookId = new LinkedHashMap<>();
        for (BookAuthor bookAuthor : bookAuthorRepository.findActiveByBookIds(bookIds)) {
            authorsByBookId.computeIfAbsent(bookAuthor.getBook().getBookId(), id -> new ArrayList<>())
                    .add(bookAuthor.getAuthor().getAuthorName());
        }
        return books.stream()
                .map(book -> new BookCardResponse(
                        book.getBookId(),
                        book.getTitle(),
                        book.getCoverImageUrl(),
                        authorsByBookId.getOrDefault(book.getBookId(), List.of()),
                        book.getPublisher() == null ? null : book.getPublisher().getPublisherName(),
                        book.getRentalType(),
                        book.getRentalPrice(),
                        defaultBigDecimal(book.getAverageRating()),
                        book.getReviewCount(),
                        false
                ))
                .toList();
    }

    private Sort sortFor(String sort) {
        if ("title".equals(sort)) {
            return Sort.by(Sort.Order.asc("title"), Sort.Order.asc("bookId"));
        }
        if ("popular".equals(sort)) {
            return Sort.by(Sort.Order.desc("rentalCount"), Sort.Order.desc("reviewCount"), Sort.Order.desc("bookId"));
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("bookId"));
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeRentalType(String rentalType) {
        String value = normalizeUpper(rentalType);
        if (value == null) {
            return null;
        }
        if (!"FREE".equals(value) && !"PAID".equals(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private String normalizeSort(String sort) {
        String value = normalizeText(sort);
        if (value == null) {
            return "latest";
        }
        if (!"latest".equals(value) && !"title".equals(value) && !"popular".equals(value)) {
            return "latest";
        }
        return value;
    }

    private String normalizeRecommendationType(String type, Long baseBookId) {
        if (baseBookId == null) {
            return "MAIN";
        }
        String value = normalizeUpper(type);
        if (value == null || "MAIN".equals(value)) {
            return "MAIN";
        }
        value = value.replace("SAME_", "");
        if (!"AUTHOR".equals(value) && !"CATEGORY".equals(value) && !"KEYWORD".equals(value)) {
            return "MAIN";
        }
        return value;
    }

    private int normalizeRecommendationLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }
        return Math.min(limit, MAX_RECOMMENDATION_LIMIT);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
