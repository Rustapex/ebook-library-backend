package com.acorn.abc.challenge.dto;

import java.time.LocalDateTime;

public record ChallengeRewardClaimResponse(
        LocalDateTime rewardClaimedAt,
        int grantedPoint
) {
}
