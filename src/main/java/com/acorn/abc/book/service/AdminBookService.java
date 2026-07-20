package com.acorn.abc.book.service;

import com.acorn.abc.admin.book.dto.AdminBookSaveRequest;
import com.acorn.abc.admin.book.dto.AdminBookSaveResponse;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeRequest;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeResponse;
import com.acorn.abc.admin.book.dto.AdminBookSummaryResponse;
import com.acorn.abc.book.domain.Author;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookAuthor;
import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.book.domain.BookDetail;
import com.acorn.abc.book.domain.Category;
import com.acorn.abc.book.domain.Publisher;
import com.acorn.abc.book.repository.AdminBookSpecifications;
import com.acorn.abc.book.repository.AuthorRepository;
import com.acorn.abc.book.repository.BookAuthorRepository;
import com.acorn.abc.book.repository.BookCategoryRepository;
import com.acorn.abc.book.repository.BookDetailRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.repository.PublisherRepository;
import com.acorn.abc.category.repository.CategoryRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 도서 CRUD(API-ADMIN-BOOK-001~004) transaction. 공개 {@link BookService}의 AVAILABLE 노출 정책,
 * {@code BookApprovalService}, 키워드·페이지 Repository/로직은 건드리지 않고 관리자 조회·등록·수정·상태변경만
 * 이 클래스에 둔다(관리자 도서 API 설계문서 §1/§4).
 */
@Service
@RequiredArgsConstructor
public class AdminBookService {

    private static final Set<String> RENTAL_TYPES = Set.of("FREE", "PAID");
    private static final Set<String> BOOK_STATUSES = Set.of("AVAILABLE", "HIDDEN", "INACTIVE");
    private static final String CATEGORY_ACTIVE = "ACTIVE";
    private static final String BOOK_INACTIVE = "INACTIVE";

    private final BookRepository bookRepository;
    private final BookDetailRepository bookDetailRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;
    private final BookLockTimeoutSupport lockTimeoutSupport;

    @Transactional(readOnly = true)
    public PageResponse<AdminBookSummaryResponse> getAdminBooks(
            String q, Long categoryId, String rentalType, String status, int page, int size) {
        String normalizedStatus = normalizeEnum(status, BOOK_STATUSES);
        String normalizedRentalType = normalizeEnum(rentalType, RENTAL_TYPES);
        String likeLower = normalizeKeyword(q);

        Specification<Book> spec = AdminBookSpecifications.hasStatus(normalizedStatus)
                .and(AdminBookSpecifications.hasRentalType(normalizedRentalType))
                .and(AdminBookSpecifications.hasCategory(categoryId))
                .and(AdminBookSpecifications.matchesKeyword(likeLower));

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("bookId")));
        Page<Book> books = bookRepository.findAll(spec, pageable);

        List<Long> bookIds = books.getContent().stream().map(Book::getBookId).toList();
        Map<Long, List<String>> authorNamesByBookId = bookIds.isEmpty() ? Map.of()
                : bookAuthorRepository.findAllByBookIdIn(bookIds).stream()
                        .collect(Collectors.groupingBy(ba -> ba.getBook().getBookId(), LinkedHashMap::new,
                                Collectors.mapping(ba -> ba.getAuthor().getAuthorName(), Collectors.toList())));
        Map<Long, List<AdminBookSummaryResponse.CategoryItem>> categoriesByBookId = bookIds.isEmpty() ? Map.of()
                : bookCategoryRepository.findAllByBookIdIn(bookIds).stream()
                        .collect(Collectors.groupingBy(bc -> bc.getBook().getBookId(), LinkedHashMap::new,
                                Collectors.mapping(bc -> new AdminBookSummaryResponse.CategoryItem(
                                        bc.getCategory().getCategoryId(), bc.getCategory().getCategoryName()),
                                        Collectors.toList())));

        List<AdminBookSummaryResponse> content = books.getContent().stream()
                .map(book -> AdminBookSummaryResponse.of(book,
                        authorNamesByBookId.getOrDefault(book.getBookId(), List.of()),
                        categoriesByBookId.getOrDefault(book.getBookId(), List.of())))
                .toList();

        return PageResponse.of(content, books.getNumber(), books.getSize(), books.getTotalElements());
    }

    @Transactional
    public AdminBookSaveResponse createAdminBook(AdminBookSaveRequest request, Long adminMemberId) {
        String title = requireTrimmed(request.title());
        String publisherName = requireTrimmed(request.publisherName());
        String isbn = normalizeIsbn(request.isbn());
        List<String> authorNames = validateAuthors(request.authors());
        List<Long> categoryIds = validateCategoryIds(request.categoryIds());
        validateRentalPrice(request.rentalType(), request.rentalPrice());

        if (isbn != null && bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.BOOK_ISBN_ALREADY_EXISTS);
        }
        List<Category> categories = resolveCategories(categoryIds);
        Publisher publisher = resolvePublisher(publisherName);
        List<Author> authors = resolveAuthors(authorNames);

        Book book = bookRepository.save(Book.create(publisher, title, isbn, trimToNull(request.coverImageUrl()),
                null, request.rentalType(), request.rentalPrice(), request.defaultRentalDays(), 0, BOOK_INACTIVE));
        bookDetailRepository.save(buildDetail(book, request.detail()));
        saveCategoryMappings(book, categories);
        saveAuthorMappings(book, authors);

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.BOOK_CREATE, AdminAuditLogTargetType.BOOK,
                book.getBookId(), null, summaryOf(book, publisher, authorNames, categoryIds));

        return new AdminBookSaveResponse(book.getBookId(), book.getStatus(), book.getCreatedAt(), book.getUpdatedAt());
    }

    @Transactional
    public AdminBookSaveResponse updateAdminBook(Long bookId, AdminBookSaveRequest request, Long adminMemberId) {
        // 계획 §3.3 순서: 입력·참조 검증(및 master 조회/생성) 먼저, write lock은 그 뒤에 최소 구간만 잡는다.
        Book existing = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        String title = requireTrimmed(request.title());
        String publisherName = requireTrimmed(request.publisherName());
        String isbn = normalizeIsbn(request.isbn());
        List<String> authorNames = validateAuthors(request.authors());
        List<Long> categoryIds = validateCategoryIds(request.categoryIds());
        validateRentalPrice(request.rentalType(), request.rentalPrice());

        if (isbn != null && !isbn.equals(existing.getIsbn()) && bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.BOOK_ISBN_ALREADY_EXISTS);
        }
        List<Category> categories = resolveCategories(categoryIds);
        Publisher publisher = resolvePublisher(publisherName);
        List<Author> authors = resolveAuthors(authorNames);

        return lockTimeoutSupport.withShortLockTimeout(() ->
                applyUpdate(bookId, title, isbn, publisher, authors, categories,
                        authorNames, categoryIds, request, adminMemberId));
    }

    private AdminBookSaveResponse applyUpdate(Long bookId, String title, String isbn, Publisher publisher,
                                               List<Author> authors, List<Category> categories,
                                               List<String> authorNames, List<Long> categoryIds,
                                               AdminBookSaveRequest request, Long adminMemberId) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        Map<String, Object> beforeSummary = summaryOf(
                book, book.getPublisher(), existingAuthorNames(bookId), existingCategoryIds(bookId));

        book.updateBasicInfo(publisher, title, isbn, trimToNull(request.coverImageUrl()),
                request.rentalType(), request.rentalPrice(), request.defaultRentalDays());
        applyDetail(book, request.detail());

        bookCategoryRepository.deleteByBook_BookId(bookId);
        bookAuthorRepository.deleteByBook_BookId(bookId);
        bookCategoryRepository.flush();
        bookAuthorRepository.flush();

        saveCategoryMappings(book, categories);
        saveAuthorMappings(book, authors);

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.BOOK_UPDATE, AdminAuditLogTargetType.BOOK,
                bookId, beforeSummary, summaryOf(book, publisher, authorNames, categoryIds));

        return new AdminBookSaveResponse(book.getBookId(), book.getStatus(), book.getCreatedAt(), book.getUpdatedAt());
    }

    @Transactional
    public AdminBookStatusChangeResponse changeAdminBookStatus(
            Long bookId, AdminBookStatusChangeRequest request, Long adminMemberId) {
        return lockTimeoutSupport.withShortLockTimeout(() -> applyStatusChange(bookId, request, adminMemberId));
    }

    private AdminBookStatusChangeResponse applyStatusChange(
            Long bookId, AdminBookStatusChangeRequest request, Long adminMemberId) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        Map<String, Object> beforeSummary = Map.of("bookId", bookId, "status", book.getStatus());
        book.changeStatus(request.status());

        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("bookId", bookId);
        afterSummary.put("status", book.getStatus());
        String reason = trimToNull(request.reason());
        if (reason != null) {
            afterSummary.put("reason", reason);
        }

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.BOOK_UPDATE, AdminAuditLogTargetType.BOOK,
                bookId, beforeSummary, afterSummary);

        return new AdminBookStatusChangeResponse(book.getBookId(), book.getStatus());
    }

    private void applyDetail(Book book, AdminBookSaveRequest.Detail detail) {
        BookDetail existingDetail = bookDetailRepository.findByBook_BookId(book.getBookId()).orElse(null);
        if (existingDetail == null) {
            bookDetailRepository.save(buildDetail(book, detail));
            return;
        }
        existingDetail.updateGuideContent(
                detail == null ? null : trimToNull(detail.description()),
                detail == null ? null : trimToNull(detail.tableOfContents()),
                detail == null ? null : trimToNull(detail.publisherReview()));
    }

    private BookDetail buildDetail(Book book, AdminBookSaveRequest.Detail detail) {
        if (detail == null) {
            return BookDetail.create(book, null, null, null, null);
        }
        return BookDetail.create(book, trimToNull(detail.description()), trimToNull(detail.tableOfContents()),
                trimToNull(detail.publisherReview()), null);
    }

    private List<String> validateAuthors(List<String> authors) {
        List<String> trimmed = authors.stream().map(String::trim).toList();
        if (trimmed.size() != new LinkedHashSet<>(trimmed).size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return trimmed;
    }

    private List<Long> validateCategoryIds(List<Long> categoryIds) {
        if (categoryIds.size() != new LinkedHashSet<>(categoryIds).size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return categoryIds;
    }

    private void validateRentalPrice(String rentalType, int rentalPrice) {
        if ("FREE".equals(rentalType) && rentalPrice != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if ("PAID".equals(rentalType) && rentalPrice < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    // 카테고리는 존재하며 ACTIVE여야 한다(계획 §3.1). "존재하지만 비활성"과 "존재하지 않음"을 구분하는
    // 전용 에러코드가 없어 BookApprovalService.resolveCategories()와 동일하게 CATEGORY_NOT_FOUND로 합친다
    // (관리자 도서 API 설계문서 §3).
    private List<Category> resolveCategories(List<Long> categoryIds) {
        List<Category> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            if (!CATEGORY_ACTIVE.equals(category.getStatus())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            categories.add(category);
        }
        return categories;
    }

    private Publisher resolvePublisher(String publisherName) {
        return publisherRepository.findByPublisherName(publisherName)
                .orElseGet(() -> publisherRepository.save(Publisher.create(publisherName)));
    }

    private List<Author> resolveAuthors(List<String> authorNames) {
        List<Author> authors = new ArrayList<>();
        for (String authorName : authorNames) {
            Author author = authorRepository.findFirstByAuthorNameOrderByAuthorIdAsc(authorName)
                    .orElseGet(() -> authorRepository.save(Author.create(authorName)));
            authors.add(author);
        }
        return authors;
    }

    private void saveCategoryMappings(Book book, List<Category> categories) {
        List<BookCategory> mappings = categories.stream().map(category -> BookCategory.create(book, category)).toList();
        bookCategoryRepository.saveAll(mappings);
    }

    private void saveAuthorMappings(Book book, List<Author> authors) {
        List<BookAuthor> mappings = new ArrayList<>();
        int order = 1;
        for (Author author : authors) {
            mappings.add(BookAuthor.create(book, author, order++));
        }
        bookAuthorRepository.saveAll(mappings);
    }

    private List<String> existingAuthorNames(Long bookId) {
        return bookAuthorRepository.findAllByBookIdIn(List.of(bookId)).stream()
                .map(ba -> ba.getAuthor().getAuthorName())
                .toList();
    }

    private List<Long> existingCategoryIds(Long bookId) {
        return bookCategoryRepository.findAllByBookIdIn(List.of(bookId)).stream()
                .map(bc -> bc.getCategory().getCategoryId())
                .toList();
    }

    // 감사 로그 before/after summary는 핵심 값만 남긴다(관리자 도서 API 설계문서 §7).
    private Map<String, Object> summaryOf(Book book, Publisher publisher, List<String> authorNames, List<Long> categoryIds) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("bookId", book.getBookId());
        summary.put("title", book.getTitle());
        summary.put("isbn", book.getIsbn());
        summary.put("status", book.getStatus());
        summary.put("rentalType", book.getRentalType());
        summary.put("rentalPrice", book.getRentalPrice());
        summary.put("defaultRentalDays", book.getDefaultRentalDays());
        summary.put("publisherName", publisher == null ? null : publisher.getPublisherName());
        summary.put("authorNames", authorNames);
        summary.put("categoryIds", categoryIds);
        return summary;
    }

    private String requireTrimmed(String value) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeIsbn(String isbn) {
        return trimToNull(isbn);
    }

    private String normalizeEnum(String value, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private String normalizeKeyword(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
