package com.acorn.abc.admin.challenge.dto;

import com.acorn.abc.challenge.dto.ChallengeRewardItemResponse;

import java.util.List;

public record AdminChallengeSummaryResponse(
        Long challengeId,
        String challengeName,
        String challengeType,
        String targetAction,
        int targetCount,
        String status,
        List<ChallengeRewardItemResponse> rewards
) {
}
