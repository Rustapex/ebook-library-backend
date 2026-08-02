package com.acorn.abc.challenge.dto;

public record ChallengeRewardItemResponse(
        String rewardType,
        Integer rewardPoint,
        int rewardQuantity
) {
}
