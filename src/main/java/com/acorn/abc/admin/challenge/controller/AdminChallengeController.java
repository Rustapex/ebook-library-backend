package com.acorn.abc.admin.challenge.controller;

import com.acorn.abc.admin.challenge.dto.AdminChallengeSummaryResponse;
import com.acorn.abc.admin.challenge.dto.AdminChallengeUpdateRequest;
import com.acorn.abc.admin.challenge.dto.AdminChallengeUpdateResponse;
import com.acorn.abc.challenge.service.ChallengeService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminChallenge", description = "관리자 챌린지 API")
@RestController
@RequestMapping("/api/v1/admin/challenges")
@RequiredArgsConstructor
@Validated
public class AdminChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "챌린지 목록 조회", description = "챌린지와 보상 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminChallengeSummaryResponse>>> getChallenges(
            @RequestParam(required = false) String challengeType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminChallengeSummaryResponse> response =
                challengeService.getAdminChallenges(challengeType, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "챌린지 수정", description = "챌린지 정보와 보상 목록을 수정합니다. 신규 생성은 이번 범위에서 제공하지 않습니다.")
    @PutMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<AdminChallengeUpdateResponse>> updateChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody AdminChallengeUpdateRequest request) {
        Long updatedId = challengeService.updateChallenge(challengeId, request);
        return ResponseEntity.ok(ApiResponse.ok(new AdminChallengeUpdateResponse(updatedId)));
    }
}
