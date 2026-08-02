package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnSeedRequest;
import com.acorn.abc.admin.book.dto.BookIsbnSeedResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.external.book.client.InfoNaruBookListClient;
import com.acorn.abc.external.book.dto.ExternalBookSeedCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 정보나루 목록 API로 ISBN 후보를 대량 수집해 book_isbn_temp에 PENDING 저장한다.
 * 이미 temp/book에 존재하는 ISBN은 건너뛰어 재실행해도 중복이 생기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class BookIsbnSeedService {

    private final InfoNaruBookListClient infoNaruBookListClient;
    private final BookIsbnTempRepository bookIsbnTempRepository;
    private final BookRepository bookRepository;

    @Transactional
    public BookIsbnSeedResponse seed(BookIsbnSeedRequest request) {
        List<String> collectedIsbns = new ArrayList<>();
        int pagesFetched = 0;

        for (int pageNo = 1; pageNo <= request.maxPages(); pageNo++) {
            pagesFetched++;
            List<ExternalBookSeedCandidate> candidates =
                    infoNaruBookListClient.search(request.keyword(), pageNo, request.pageSize());

            if (candidates.isEmpty()) {
                break;
            }

            for (ExternalBookSeedCandidate candidate : candidates) {
                if (bookIsbnTempRepository.findByIsbn13(candidate.isbn13()).isPresent()) {
                    continue;
                }
                if (bookRepository.existsByIsbn(candidate.isbn13())) {
                    continue;
                }

                bookIsbnTempRepository.save(
                        BookIsbnTemp.createSeed(candidate.isbn13(), candidate.title(), candidate.publisher()));
                collectedIsbns.add(candidate.isbn13());

                if (collectedIsbns.size() >= request.targetCount()) {
                    return new BookIsbnSeedResponse(
                            request.targetCount(), collectedIsbns.size(), pagesFetched, collectedIsbns);
                }
            }
        }

        return new BookIsbnSeedResponse(request.targetCount(), collectedIsbns.size(), pagesFetched, collectedIsbns);
    }
}
