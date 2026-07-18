package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookIsbnTempBulkDeleteRequest(
        @NotEmpty(message = "삭제할 수집 후보 ID는 필수입니다.")
        @Size(max = 100, message = "수집 후보는 최대 100개까지 삭제할 수 있습니다.")
        List<@NotNull(message = "수집 후보 ID는 null일 수 없습니다.")
                @Positive(message = "수집 후보 ID는 양수여야 합니다.") Long> tempIds
) {
}
