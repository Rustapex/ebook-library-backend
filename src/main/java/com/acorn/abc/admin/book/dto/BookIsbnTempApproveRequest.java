package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record BookIsbnTempApproveRequest(
        @NotBlank(message = "title is required") String title,
        @NotBlank(message = "publisherName is required") String publisherName,
        @NotEmpty(message = "at least one author is required")
        List<@NotBlank(message = "author name must not be blank") String> authors,
        List<Long> categoryIds,
        @NotNull(message = "rentalType is required")
        @Pattern(regexp = "FREE|PAID", message = "rentalType must be FREE or PAID") String rentalType,
        @NotNull(message = "rentalPrice is required")
        @Min(value = 0, message = "rentalPrice must be zero or greater") Integer rentalPrice,
        @NotBlank(message = "status is required")
        @Pattern(regexp = "AVAILABLE|HIDDEN|INACTIVE", message = "invalid book status") String status,
        Detail detail
) {
    public record Detail(
            String description,
            String tableOfContents,
            String publisherReview,
            String detailContent
    ) {
    }
}
