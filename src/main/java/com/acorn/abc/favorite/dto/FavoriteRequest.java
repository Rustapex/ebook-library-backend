package com.acorn.abc.favorite.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull(message = "도서 번호는 필수입니다.")
        Long bookId
) {
}
