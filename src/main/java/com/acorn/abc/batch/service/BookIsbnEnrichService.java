package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnEnrichResponse;
import com.acorn.abc.admin.book.dto.BookIsbnTempFetchResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PENDING temp 후보를 기존 fetch() 파이프라인으로 순차 보강한다.
 * 알라딘은 단일 키로 순차 호출하며, 건별 실패가 나머지 후보 처리를 막지 않는다.
 * (다중 키 로테이션/쿨다운/전역 정지 컨트롤러는 이번 시연 범위에서 제외)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookIsbnEnrichService {

    private static final long CALL_INTERVAL_MS = 200;

    private final BookIsbnTempRepository bookIsbnTempRepository;
    private final BookIsbnTempService bookIsbnTempService;

    public BookIsbnEnrichResponse enrich(int limit) {
        Page<BookIsbnTemp> pendingPage = bookIsbnTempRepository.findByStatusCd(
                BookIsbnTemp.STATUS_PENDING,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt")));

        int readyCount = 0;
        List<String> failedIsbns = new ArrayList<>();

        for (BookIsbnTemp temp : pendingPage.getContent()) {
            String isbn13 = temp.getIsbn13();
            try {
                BookIsbnTempFetchResponse response = bookIsbnTempService.fetch(isbn13);
                if (response.statusCd() == BookIsbnTemp.STATUS_READY) {
                    readyCount++;
                } else {
                    failedIsbns.add(isbn13);
                }
            } catch (Exception e) {
                log.warn("enrich 실패 isbn13={}, message={}", isbn13, e.getMessage());
                failedIsbns.add(isbn13);
            }
            sleep();
        }

        return new BookIsbnEnrichResponse(pendingPage.getContent().size(), readyCount, failedIsbns);
    }

    private void sleep() {
        try {
            Thread.sleep(CALL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
