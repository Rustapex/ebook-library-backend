package com.acorn.abc.admin.book.dto;

import java.util.List;

/**
 * seed 수집 결과. collectedIsbns는 이번 요청에서 새로 PENDING 저장된 ISBN 목록이다.
 */
public record BookIsbnSeedResponse(
        int targetCount,
        int collectedCount,
        int pagesFetched,
        List<String> collectedIsbns
) {
}
