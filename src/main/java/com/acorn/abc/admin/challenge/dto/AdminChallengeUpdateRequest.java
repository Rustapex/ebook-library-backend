package com.acorn.abc.admin.challenge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;

import java.util.List;

public record AdminChallengeUpdateRequest(
        @NotBlank(message = "챌린지명은 필수입니다.")
        String challengeName,

        @NotBlank(message = "챌린지 유형은 필수입니다.")
        String challengeType,

        @NotBlank(message = "목표 행동은 필수입니다.")
        String targetAction,

        @Min(value = 1, message = "목표 횟수는 1 이상이어야 합니다.")
        int targetCount,

        @NotBlank(message = "상태는 필수입니다.")
        String status,

        @NotEmpty(message = "보상을 1개 이상 입력해주세요.")
        @Valid
        List<AdminChallengeRewardItemRequest> rewards
) {
}
