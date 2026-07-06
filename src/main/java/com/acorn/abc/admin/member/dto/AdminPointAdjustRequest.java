package com.acorn.abc.admin.member.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminPointAdjustRequest(
        int pointAmount,

        @NotBlank(message = "조정 사유는 필수입니다.")
        String description
) {
}
