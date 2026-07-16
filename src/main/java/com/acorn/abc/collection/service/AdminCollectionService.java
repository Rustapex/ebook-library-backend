package com.acorn.abc.collection.service;

import com.acorn.abc.admin.collection.dto.AdminCollectionBookResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionDetailResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSummaryResponse;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.collection.domain.Collection;
import com.acorn.abc.collection.domain.CollectionBook;
import com.acorn.abc.collection.repository.CollectionBookRepository;
import com.acorn.abc.collection.repository.CollectionRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 컬렉션 CRUD transaction. 공개 {@link CollectionService}의 ACTIVE/AVAILABLE 노출 정책은
 * 건드리지 않고, 관리자 조회·생성·수정·도서 구성·상태 변경만 이 클래스에 둔다
 * (관리자 컬렉션 API 구현계획 2026-07-16 §5.1).
 */
@Service
@RequiredArgsConstructor
public class AdminCollectionService {

    private static final String SERIES = "SERIES";
    private static final Set<String> COLLECTION_TYPES = Set.of("SERIES", "EVENT");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "HIDDEN", "ENDED");

    private final CollectionRepository collectionRepository;
    private final CollectionBookRepository collectionBookRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional(readOnly = true)
    public PageResponse<AdminCollectionSummaryResponse> getAdminCollections(
            String collectionType, String status, int page, int size) {
        String normalizedType = normalizeOptionalEnum(collectionType, COLLECTION_TYPES);
        String normalizedStatus = normalizeOptionalEnum(status, STATUSES);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("collectionId")));

        Page<Collection> collections = collectionRepository.searchForAdmin(normalizedType, normalizedStatus, pageable);
        List<Long> collectionIds = collections.getContent().stream().map(Collection::getCollectionId).toList();
        Map<Long, Long> mappedCountByCollectionId = collectionIds.isEmpty()
                ? Map.of()
                : collectionBookRepository.countByCollectionIds(collectionIds).stream()
                        .collect(Collectors.toMap(
                                CollectionBookRepository.CollectionMappedCountView::getCollectionId,
                                CollectionBookRepository.CollectionMappedCountView::getMappedCount));

        List<AdminCollectionSummaryResponse> content = collections.getContent().stream()
                .map(collection -> AdminCollectionSummaryResponse.of(collection,
                        mappedCountByCollectionId.getOrDefault(collection.getCollectionId(), 0L)))
                .toList();
        return PageResponse.of(content, collections.getNumber(), collections.getSize(), collections.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminCollectionDetailResponse getAdminCollectionDetail(Long collectionId) {
        Collection collection = findCollectionOrThrow(collectionId);
        List<CollectionBook> collectionBooks = SERIES.equals(collection.getCollectionType())
                ? collectionBookRepository.findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(collectionId)
                : collectionBookRepository.findByCollection_CollectionIdOrderByCreatedAtAscCollectionBookIdAsc(collectionId);
        List<AdminCollectionBookResponse> books = collectionBooks.stream()
                .map(AdminCollectionBookResponse::from)
                .toList();
        return AdminCollectionDetailResponse.of(collection, books);
    }

    public AdminCollectionSaveResponse createAdminCollection(AdminCollectionSaveRequest request, Long adminMemberId) {
        String name = validateAndTrimName(request.name());
        validateFieldsByType(request.collectionType(), request.discountRate(), request.startDate(), request.endDate());

        Member admin = adminMemberId == null ? null : memberRepository.getReferenceById(adminMemberId);
        Collection collection = Collection.create(name, request.collectionType(), request.discountRate(),
                request.description(), request.startDate(), request.endDate(), request.status(), admin);
        collectionRepository.save(collection);

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COLLECTION_CREATE,
                AdminAuditLogTargetType.COLLECTION, collection.getCollectionId(), null, summaryOf(collection, 0L));

        return new AdminCollectionSaveResponse(collection.getCollectionId(), collection.getStatus(),
                collection.getCreatedAt(), collection.getUpdatedAt());
    }

    public AdminCollectionSaveResponse updateAdminCollection(
            Long collectionId, AdminCollectionSaveRequest request, Long adminMemberId) {
        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        String name = validateAndTrimName(request.name());
        validateFieldsByType(request.collectionType(), request.discountRate(), request.startDate(), request.endDate());

        // D-08: mapping이 하나라도 있으면 collectionType 변경을 거부한다.
        if (!collection.getCollectionType().equals(request.collectionType())
                && collectionBookRepository.existsByCollection_CollectionId(collectionId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        long mappedCount = collectionBookRepository.countByCollection_CollectionId(collectionId);
        Map<String, Object> beforeSummary = summaryOf(collection, mappedCount);

        collection.updateBasicInfo(name, request.collectionType(), request.discountRate(), request.description(),
                request.startDate(), request.endDate(), request.status());

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COLLECTION_UPDATE,
                AdminAuditLogTargetType.COLLECTION, collectionId, beforeSummary, summaryOf(collection, mappedCount));

        return new AdminCollectionSaveResponse(collection.getCollectionId(), collection.getStatus(),
                collection.getCreatedAt(), collection.getUpdatedAt());
    }

    public AdminCollectionBooksUpdateResponse upsertCollectionBooks(
            Long collectionId, AdminCollectionBooksUpdateRequest request, Long adminMemberId) {
        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        List<AdminCollectionBooksUpdateRequest.BookItem> items = request.books();
        List<Long> requestedBookIds = items.stream().map(AdminCollectionBooksUpdateRequest.BookItem::bookId).toList();
        if (requestedBookIds.size() != new HashSet<>(requestedBookIds).size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        List<Book> books = bookRepository.findAllById(requestedBookIds);
        if (books.size() != requestedBookIds.size()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        Map<Long, Book> bookById = books.stream().collect(Collectors.toMap(Book::getBookId, book -> book));

        boolean isSeries = SERIES.equals(collection.getCollectionType());
        validateDisplayOrderByType(isSeries, items);
        if (isSeries) {
            validateSeriesDisplayOrderUniqueness(collectionId, requestedBookIds, items);
        }

        List<CollectionBook> existingMappings =
                collectionBookRepository.findByCollection_CollectionIdAndBook_BookIdIn(collectionId, requestedBookIds);
        Map<Long, CollectionBook> existingByBookId = existingMappings.stream()
                .collect(Collectors.toMap(cb -> cb.getBook().getBookId(), cb -> cb));

        for (AdminCollectionBooksUpdateRequest.BookItem item : items) {
            CollectionBook existing = existingByBookId.get(item.bookId());
            if (existing != null) {
                existing.changeDisplayOrder(item.displayOrder());
            } else {
                collectionBookRepository.save(CollectionBook.create(collection, bookById.get(item.bookId()), item.displayOrder()));
            }
        }

        long mappedCount = collectionBookRepository.countByCollection_CollectionId(collectionId);
        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("collectionId", collectionId);
        afterSummary.put("mappedCount", mappedCount);
        afterSummary.put("changedBookIds", requestedBookIds);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COLLECTION_BOOKS_UPDATE,
                AdminAuditLogTargetType.COLLECTION, collectionId, null, afterSummary);

        return new AdminCollectionBooksUpdateResponse(collectionId, mappedCount);
    }

    public AdminCollectionPatchResponse patchCollection(
            Long collectionId, AdminCollectionPatchRequest request, Long adminMemberId) {
        boolean hasStatus = request.status() != null;
        boolean hasRemoveBookIds = request.removeBookIds() != null && !request.removeBookIds().isEmpty();
        if (!hasStatus && !hasRemoveBookIds) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.removeBookIds() != null
                && request.removeBookIds().size() != new HashSet<>(request.removeBookIds()).size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        long mappedCountBefore = collectionBookRepository.countByCollection_CollectionId(collectionId);
        Map<String, Object> beforeSummary = summaryOf(collection, mappedCountBefore);

        if (hasStatus) {
            collection.changeStatus(request.status());
        }
        if (hasRemoveBookIds) {
            // idempotent — 이미 제거됐거나 애초에 없던 bookId는 그냥 0건 삭제로 조용히 무시된다.
            collectionBookRepository.deleteByCollection_CollectionIdAndBook_BookIdIn(collectionId, request.removeBookIds());
        }

        long mappedCountAfter = collectionBookRepository.countByCollection_CollectionId(collectionId);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COLLECTION_PATCH,
                AdminAuditLogTargetType.COLLECTION, collectionId, beforeSummary, summaryOf(collection, mappedCountAfter));

        return new AdminCollectionPatchResponse(collectionId, collection.getStatus(), mappedCountAfter);
    }

    private Collection findCollectionOrThrow(Long collectionId) {
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
    }

    private String validateAndTrimName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return trimmed;
    }

    // D-07 확정: SERIES는 discountRate/startDate/endDate를 null로 제한하고, EVENT는 두 날짜가
    // 모두 있으면 startDate<=endDate만 검증한다(할인율 0~100 범위는 DTO의 @DecimalMin/@DecimalMax가 담당).
    private void validateFieldsByType(String collectionType, java.math.BigDecimal discountRate,
                                       java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (SERIES.equals(collectionType)) {
            if (discountRate != null || startDate != null || endDate != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        } else if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateDisplayOrderByType(boolean isSeries, List<AdminCollectionBooksUpdateRequest.BookItem> items) {
        if (isSeries) {
            boolean anyInvalid = items.stream()
                    .anyMatch(item -> item.displayOrder() == null || item.displayOrder() <= 0);
            if (anyInvalid) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        } else if (items.stream().anyMatch(item -> item.displayOrder() != null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    // 이번 요청으로 바뀌지 않는 기존 mapping의 displayOrder까지 합쳐 컬렉션 전체 기준으로 중복을 막는다(D-07).
    private void validateSeriesDisplayOrderUniqueness(
            Long collectionId, List<Long> requestedBookIds, List<AdminCollectionBooksUpdateRequest.BookItem> items) {
        Set<Integer> finalOrders = new HashSet<>();
        collectionBookRepository.findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(collectionId).stream()
                .filter(cb -> !requestedBookIds.contains(cb.getBook().getBookId()))
                .forEach(cb -> finalOrders.add(cb.getDisplayOrder()));
        for (AdminCollectionBooksUpdateRequest.BookItem item : items) {
            if (!finalOrders.add(item.displayOrder())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private String normalizeOptionalEnum(String value, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    // 감사 로그 before/after summary는 개인정보나 전체 description 없이 ID/type/status/mappedCount만 남긴다(§5.5).
    private Map<String, Object> summaryOf(Collection collection, long mappedCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("collectionId", collection.getCollectionId());
        summary.put("collectionType", collection.getCollectionType());
        summary.put("status", collection.getStatus());
        summary.put("mappedCount", mappedCount);
        return summary;
    }
}
