package com.acorn.abc.external.library.dto;

import java.util.List;

// libSrchByBook 단건 호출 결과. items는 데이터가 없으면 빈 list(무데이터와 실패를 status로 구분한다).
public record InfoNaruLibrarySearchResult(
        InfoNaruLibraryFetchStatus status,
        List<InfoNaruLibraryItem> items,
        int totalCount
) {

    public static InfoNaruLibrarySearchResult notTried() {
        return new InfoNaruLibrarySearchResult(InfoNaruLibraryFetchStatus.NOT_TRIED, List.of(), 0);
    }

    public static InfoNaruLibrarySearchResult noData() {
        return new InfoNaruLibrarySearchResult(InfoNaruLibraryFetchStatus.SUCCESS_NO_DATA, List.of(), 0);
    }

    public static InfoNaruLibrarySearchResult failedRetryable() {
        return new InfoNaruLibrarySearchResult(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE, List.of(), 0);
    }

    public static InfoNaruLibrarySearchResult success(List<InfoNaruLibraryItem> items, int totalCount) {
        return new InfoNaruLibrarySearchResult(InfoNaruLibraryFetchStatus.SUCCESS_HAS_DATA, items, totalCount);
    }
}
