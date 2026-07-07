package com.acorn.abc.admin.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPointAdjustRequest(
        @NotNull(message = "포인트 조정 값은 필수입니다.")
        Integer pointAmount,

        @NotBlank(message = "조정 사유는 필수입니다.")
        String description
) {
}
