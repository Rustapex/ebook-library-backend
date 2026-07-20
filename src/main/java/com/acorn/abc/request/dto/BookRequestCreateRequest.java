package com.acorn.abc.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequestCreateRequest(
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 255, message = "제목은 255자 이하여야 합니다.") String title,
        @NotBlank(message = "저자는 필수입니다.") @Size(max = 100, message = "저자는 100자 이하여야 합니다.") String author,
        @NotBlank(message = "출판사는 필수입니다.") @Size(max = 100, message = "출판사는 100자 이하여야 합니다.") String publisher,
        @NotBlank(message = "신청 사유는 필수입니다.") String reason
) {
}
