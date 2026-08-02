package com.acorn.abc.challenge.controller;

import com.acorn.abc.challenge.dto.ChallengeResponse;
import com.acorn.abc.challenge.dto.ChallengeRewardClaimResponse;
import com.acorn.abc.challenge.service.ChallengeService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Challenge", description = "챌린지 API")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "챌린지 목록 조회", description = "진행률/보상 정보를 포함한 챌린지 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getChallenges(
            @RequestParam(required = false) String challengeType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChallengeResponse> response = challengeService.getChallenges(userDetails.getMemberId(), challengeType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "챌린지 보상 수령", description = "달성했고 미수령인 챌린지의 보상을 지급합니다.")
    @PostMapping("/{challengeId}/reward")
    public ResponseEntity<ApiResponse<ChallengeRewardClaimResponse>> claimReward(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChallengeRewardClaimResponse response = challengeService.claimReward(userDetails.getMemberId(), challengeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
