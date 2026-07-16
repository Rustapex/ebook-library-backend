package com.acorn.abc.admin.collection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCollectionBooksUpdateRequest(

        @NotEmpty(message = "도서 목록은 비어 있을 수 없습니다.")
        @Valid
        List<BookItem> books
) {
    public record BookItem(
            @NotNull(message = "bookId는 필수입니다.")
            Long bookId,

            // SERIES는 필수(양수, 컬렉션 내 중복 불가), EVENT는 null이어야 한다(D-07) — Service에서 검증한다.
            Integer displayOrder
    ) {
    }
}
