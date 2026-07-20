package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnBulkApproveRequest;
import com.acorn.abc.admin.book.dto.BookIsbnBulkApproveResponse;
import com.acorn.abc.admin.book.dto.BookIsbnTempApproveRequest;
import com.acorn.abc.admin.book.dto.BookIsbnTempApproveResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * READY temp 후보를 일괄 승인한다. 승인 자체는 기존 BookApprovalService.approve()를
 * 건별로 재사용하며(별도 빈 호출이라 건별 @Transactional 유지), 이 서비스에는
 * @Transactional을 붙이지 않아 한 건 실패가 이미 승인된 다른 책까지 rollback시키지 않는다.
 * limit(승인시킬 최대 책 수)과 별개로 maxCandidates(검사할 최대 temp 수)로 스캔 범위 자체를 제한한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookBulkApprovalService {

    private final BookIsbnTempRepository bookIsbnTempRepository;
    private final BookRepository bookRepository;
    private final BookApprovalService bookApprovalService;

    public BookIsbnBulkApproveResponse bulkApprove(BookIsbnBulkApproveRequest request, Long adminMemberId) {
        List<BookIsbnTemp> candidates = resolveCandidates(request.tempIds(), request.maxCandidates());

        int approvedCount = 0;
        int examinedCount = 0;
        List<Long> approvedBookIds = new ArrayList<>();
        List<BookIsbnBulkApproveResponse.Failure> failures = new ArrayList<>();

        for (BookIsbnTemp temp : candidates) {
            if (approvedCount >= request.limit() || examinedCount >= request.maxCandidates()) {
                break;
            }
            examinedCount++;

            String skipReason = eligibilityFailureReason(temp);
            if (skipReason != null) {
                failures.add(new BookIsbnBulkApproveResponse.Failure(temp.getIsbn13(), skipReason));
                continue;
            }

            List<String> authors = AuthorNameParser.parse(temp.getAuthor());
            BookIsbnTempApproveRequest approveRequest = buildApproveRequest(temp, authors, request);

            try {
                BookIsbnTempApproveResponse response =
                        bookApprovalService.approve(temp.getId(), approveRequest, adminMemberId);
                approvedCount++;
                approvedBookIds.add(response.bookId());
            } catch (BusinessException e) {
                log.warn("bulk approve 실패 isbn13={}, code={}", temp.getIsbn13(), e.getErrorCode().getCode());
                failures.add(new BookIsbnBulkApproveResponse.Failure(
                        temp.getIsbn13(), e.getErrorCode().getMessage()));
            }
        }

        return new BookIsbnBulkApproveResponse(approvedCount, approvedBookIds, failures);
    }

    private List<BookIsbnTemp> resolveCandidates(List<Long> tempIds, int maxCandidates) {
        if (tempIds != null && !tempIds.isEmpty()) {
            return bookIsbnTempRepository.findAllById(tempIds);
        }
        Page<BookIsbnTemp> readyPage = bookIsbnTempRepository.findByStatusCd(
                BookIsbnTemp.STATUS_READY,
                PageRequest.of(0, maxCandidates, Sort.by(Sort.Direction.ASC, "createdAt")));
        return readyPage.getContent();
    }

    private String eligibilityFailureReason(BookIsbnTemp temp) {
        if (temp.getStatusCd() != BookIsbnTemp.STATUS_READY) {
            return "READY 상태가 아님";
        }
        if (isBlank(temp.getBookTitle())) {
            return "제목 없음";
        }
        if (isBlank(temp.getPublisher())) {
            return "출판사 없음";
        }
        if (AuthorNameParser.parse(temp.getAuthor()).isEmpty()) {
            return "저자 없음";
        }
        if (isBlank(temp.getAladinCategoryId()) || isBlank(temp.getAladinCategoryName())) {
            return "알라딘 카테고리 없음";
        }
        if (bookRepository.existsByIsbn(temp.getIsbn13())) {
            return "이미 등록된 ISBN";
        }
        return null;
    }

    private BookIsbnTempApproveRequest buildApproveRequest(BookIsbnTemp temp, List<String> authors,
                                                            BookIsbnBulkApproveRequest request) {
        BookIsbnTempApproveRequest.Detail detail =
                new BookIsbnTempApproveRequest.Detail(temp.getDescription(), null, null, null);

        return new BookIsbnTempApproveRequest(
                temp.getBookTitle(),
                temp.getPublisher(),
                authors,
                List.of(),
                request.rentalType(),
                request.rentalPrice(),
                request.status(),
                detail
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
