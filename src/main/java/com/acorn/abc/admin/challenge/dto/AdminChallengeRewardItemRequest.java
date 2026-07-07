package com.acorn.abc.admin.challenge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminChallengeRewardItemRequest(
        @NotBlank(message = "보상 유형은 필수입니다.")
        @Pattern(regexp = "POINT", message = "보상 유형은 POINT만 허용됩니다.")
        String rewardType,

        Integer rewardPoint,

        Long couponId,

        @Min(value = 1, message = "보상 수량은 1개 이상이어야 합니다.")
        int rewardQuantity
) {
}
