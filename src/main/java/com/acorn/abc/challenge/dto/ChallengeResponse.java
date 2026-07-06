package com.acorn.abc.challenge.dto;

import java.util.List;

public record ChallengeResponse(
        Long challengeId,
        String challengeName,
        String challengeType,
        String targetAction,
        int targetCount,
        int currentCount,
        boolean achievedYn,
        boolean rewardClaimedYn,
        List<ChallengeRewardItemResponse> rewards
) {
}
