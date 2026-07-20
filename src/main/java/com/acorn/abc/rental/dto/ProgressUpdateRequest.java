package com.acorn.abc.rental.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProgressUpdateRequest(
        @NotNull @Positive Integer epubVersion,
        @NotBlank @Size(max = 4000) String cfi,
        @Size(max = 1000) String chapterHref,
        @NotNull @PositiveOrZero Integer spineIndex,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double progression,
        @NotNull Boolean atEnd,
        @NotNull @PositiveOrZero Long expectedRevision
) {
}
