package com.acorn.abc.admin.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// requestId는 회원별 멱등 키(구현계획 §1.6) — 같은 memberId+requestId 재요청은 기존 결과를 반환한다.
public record AdminPointAdjustRequest(
        @NotNull(message = "포인트 조정 값은 필수입니다.")
        Integer pointAmount,

        @NotBlank(message = "조정 사유는 필수입니다.")
        String description,

        @NotBlank(message = "requestId는 필수입니다.")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "requestId는 UUID 형식이어야 합니다.")
        String requestId
) {
}
