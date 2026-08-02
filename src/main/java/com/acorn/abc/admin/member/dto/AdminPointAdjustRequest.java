package com.acorn.abc.admin.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// requestId는 회원별 멱등 키(구현계획 §1.6) — 같은 memberId+requestId 재요청은 기존 결과를 반환한다.
// PR51 코드리뷰 대응 §2.3-1 — 세 필드 모두 Swagger에 명시적으로 필수로 표기한다.
public record AdminPointAdjustRequest(
        @NotNull(message = "포인트 조정 값은 필수입니다.")
        @Schema(description = "조정할 포인트 값(양수는 지급, 음수는 차감)", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer pointAmount,

        @NotBlank(message = "조정 사유는 필수입니다.")
        @Schema(description = "조정 사유", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @NotBlank(message = "requestId는 필수입니다.")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "requestId는 UUID 형식이어야 합니다.")
        @Schema(description = "회원별 멱등 키(UUID). 같은 memberId+requestId 재요청은 최초 성공 응답을 그대로 반환합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
        String requestId
) {
}
