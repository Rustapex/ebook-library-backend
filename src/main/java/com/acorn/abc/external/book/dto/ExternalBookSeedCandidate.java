package com.acorn.abc.external.book.dto;

/**
 * 정보나루 목록 검색 결과에서 뽑은 ISBN 후보(제목/출판사 포함).
 */
public record ExternalBookSeedCandidate(
        String isbn13,
        String title,
        String publisher
) {
}
