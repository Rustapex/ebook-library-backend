package com.acorn.abc.collection.service;

import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.collection.domain.Collection;
import com.acorn.abc.collection.dto.CollectionCriteria;
import com.acorn.abc.collection.dto.CollectionDetailResponse;
import com.acorn.abc.collection.dto.CollectionSummaryResponse;
import com.acorn.abc.collection.repository.CollectionRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private static final String ACTIVE = "ACTIVE";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_PREVIEW_SIZE = 12;

    private final CollectionRepository collectionRepository;
    private final BookService bookService;

    public PageResponse<CollectionSummaryResponse> getCollections(
            String collectionType,
            String status,
            Integer page,
            Integer size,
            Integer previewSize
    ) {
        if (status != null && !ACTIVE.equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        CollectionCriteria criteria = new CollectionCriteria(
                normalizeCollectionType(collectionType),
                normalizePage(page),
                normalizeSize(size),
                normalizePreviewSize(previewSize)
        );

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort());
        Page<Collection> collections = criteria.collectionType() == null
                ? collectionRepository.findByStatus(ACTIVE, pageable)
                : collectionRepository.findByStatusAndCollectionType(ACTIVE, criteria.collectionType(), pageable);

        List<CollectionSummaryResponse> content = collections.getContent().stream()
                .map(collection -> toSummary(collection, criteria.previewSize()))
                .toList();
        return PageResponse.of(content, collections.getNumber(), collections.getSize(), collections.getTotalElements());
    }

    public CollectionDetailResponse getCollectionDetail(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .filter(candidate -> ACTIVE.equals(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        List<BookCardResponse> books = bookService.findCardsByCollectionId(collectionId, null);
        return new CollectionDetailResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCollectionType(),
                collection.getDiscountRate(),
                collection.getDescription(),
                collection.getStartDate(),
                collection.getEndDate(),
                books
        );
    }

    private CollectionSummaryResponse toSummary(Collection collection, Integer previewSize) {
        List<BookCardResponse> previewBooks = previewSize == null
                ? List.of()
                : bookService.findCardsByCollectionId(collection.getCollectionId(), previewSize);
        return new CollectionSummaryResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCollectionType(),
                collection.getDiscountRate(),
                collection.getDescription(),
                collection.getStartDate(),
                collection.getEndDate(),
                previewBooks
        );
    }

    private Sort sort() {
        return Sort.by(
                Sort.Order.desc("startDate").nullsLast(),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("collectionId")
        );
    }

    private String normalizeCollectionType(String collectionType) {
        if (collectionType == null || collectionType.isBlank()) {
            return null;
        }
        String value = collectionType.trim().toUpperCase(Locale.ROOT);
        if (!"SERIES".equals(value) && !"EVENT".equals(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private Integer normalizePreviewSize(Integer previewSize) {
        if (previewSize == null) {
            return null;
        }
        if (previewSize <= 0) {
            return 0;
        }
        return Math.min(previewSize, MAX_PREVIEW_SIZE);
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
}
