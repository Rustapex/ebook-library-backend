package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 정보나루 목록 API로 ISBN 후보를 대량 수집(seed)하는 요청.
 * targetCount를 채우거나 maxPages에 도달하면 수집을 종료한다.
 */
public record BookIsbnSeedRequest(
        @NotBlank(message = "검색 키워드는 필수입니다.")
        String keyword,

        @NotNull(message = "목표 수집 건수는 필수입니다.")
        @Min(value = 1, message = "목표 수집 건수는 1 이상이어야 합니다.")
        Integer targetCount,

        @NotNull(message = "페이지 크기는 필수입니다.")
        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer pageSize,

        @NotNull(message = "최대 페이지 수는 필수입니다.")
        @Min(value = 1, message = "최대 페이지 수는 1 이상이어야 합니다.")
        Integer maxPages
) {
}
