package com.acorn.abc.external.library.dto;

// InfoNaruLibraryClient 호출 결과 상태. book 패키지의 ProviderStatus와 별개로 관리한다
// (library 도메인은 대여 가능 여부가 아닌 소장 여부만 다루므로 book 상태 체계를 그대로 재사용하지 않는다).
public enum InfoNaruLibraryFetchStatus {
    NOT_TRIED,
    SUCCESS_HAS_DATA,
    SUCCESS_NO_DATA,
    FAILED_RETRYABLE
}
