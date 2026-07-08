package com.acorn.abc.challenge.service;

import com.acorn.abc.admin.challenge.dto.AdminChallengeRewardItemRequest;
import com.acorn.abc.admin.challenge.dto.AdminChallengeSummaryResponse;
import com.acorn.abc.admin.challenge.dto.AdminChallengeUpdateRequest;
import com.acorn.abc.challenge.domain.Challenge;
import com.acorn.abc.challenge.domain.ChallengeProgress;
import com.acorn.abc.challenge.domain.ChallengeReward;
import com.acorn.abc.challenge.dto.ChallengeResponse;
import com.acorn.abc.challenge.dto.ChallengeRewardClaimResponse;
import com.acorn.abc.challenge.dto.ChallengeRewardItemResponse;
import com.acorn.abc.challenge.repository.ChallengeProgressRepository;
import com.acorn.abc.challenge.repository.ChallengeRepository;
import com.acorn.abc.challenge.repository.ChallengeRewardRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * target_action="ATTENDANCE"는 api-spec/화면흐름도에 명시된 고정값이 아니라,
 * 출석 트리거 진행도 갱신을 구현하기 위해 이번 작업에서 정한 컨벤션이다.
 * 관리자가 출석 연동 챌린지를 등록할 때는 targetAction을 반드시 "ATTENDANCE"로 입력해야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeService {

    public static final String ATTENDANCE_TARGET_ACTION = "ATTENDANCE";

    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final ChallengeRewardRepository challengeRewardRepository;
    private final MemberRepository memberRepository;
    private final PointService pointService;

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallenges(Long memberId, String challengeType) {
        List<Challenge> challenges = (challengeType == null || challengeType.isBlank())
                ? challengeRepository.findByStatus("ACTIVE")
                : challengeRepository.findByStatusAndChallengeType("ACTIVE", challengeType);

        Map<Long, List<ChallengeRewardItemResponse>> rewardsByChallengeId = toRewardItemsByChallengeIds(challenges);
        Map<Long, ChallengeProgress> progressByChallengeId = toProgressByChallengeId(memberId, challenges);
        return challenges.stream()
                .map(challenge -> toResponse(challenge,
                        rewardsByChallengeId.getOrDefault(challenge.getChallengeId(), List.of()),
                        progressByChallengeId.get(challenge.getChallengeId())))
                .toList();
    }

    /** DAILY/TOTAL 그룹별 batch 조회로 챌린지 목록 progress N+1을 방지한다. */
    private Map<Long, ChallengeProgress> toProgressByChallengeId(Long memberId, List<Challenge> challenges) {
        List<Long> dailyIds = challenges.stream()
                .filter(challenge -> "DAILY".equals(challenge.getChallengeType()))
                .map(Challenge::getChallengeId)
                .toList();
        List<Long> totalIds = challenges.stream()
                .filter(challenge -> "TOTAL".equals(challenge.getChallengeType()))
                .map(Challenge::getChallengeId)
                .toList();

        List<ChallengeProgress> dailyProgress = dailyIds.isEmpty() ? List.of()
                : challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
                        memberId, dailyIds, LocalDate.now());
        List<ChallengeProgress> totalProgress = totalIds.isEmpty() ? List.of()
                : challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdIn(memberId, totalIds);

        return Stream.concat(dailyProgress.stream(), totalProgress.stream())
                .collect(Collectors.toMap(
                        progress -> progress.getChallenge().getChallengeId(),
                        progress -> progress,
                        (first, second) -> first));
    }

    // 정책: 챌린지 보상은 "달성 시점"이 아니라 "수령 시점"에 조회한 최신 challenge_reward를 지급한다(85행 조회).
    // 즉 달성 후 미수령 상태에서 관리자가 보상을 변경하면, 이후 수령 시 변경된 보상이 지급된다(의도된 동작).
    // 이 정책을 스냅샷 방식으로 바꿔야 한다면 별도 계획 문서 작성 후 진행할 것(2026-07-08_PR33_2차코드리뷰_대응계획.md 참고).
    public ChallengeRewardClaimResponse claimReward(Long memberId, Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        ChallengeProgress progress = findProgress(memberId, challenge).orElse(null);
        if (progress == null || !Boolean.TRUE.equals(progress.getAchievedYn()) || progress.getRewardClaimedAt() != null) {
            throw new BusinessException(ErrorCode.CHALLENGE_REWARD_NOT_CLAIMABLE);
        }

        // 동시 요청 중복 지급 방지: 지급 전에 먼저 원자적으로 "미수령" 상태를 선점한다.
        // 영향받은 row가 0건이면 다른 요청이 이미 선점한 것이므로 여기서 막는다.
        LocalDateTime claimedAt = LocalDateTime.now();
        int updatedRows = challengeProgressRepository.markRewardClaimedIfUnclaimed(
                progress.getChallengeProgressId(), claimedAt);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.CHALLENGE_REWARD_NOT_CLAIMABLE);
        }

        int grantedPoint = 0;
        List<ChallengeReward> rewards = challengeRewardRepository.findByChallenge_ChallengeId(challengeId);
        for (ChallengeReward reward : rewards) {
            if ("POINT".equals(reward.getRewardType()) && reward.getRewardPoint() != null) {
                int amount = reward.getRewardPoint() * reward.getRewardQuantity();
                pointService.grantOrAdjustPoint(memberId, amount, "challenge_reward",
                        challenge.getChallengeName() + " 챌린지 보상");
                grantedPoint += amount;
            }
            // TODO: rewardType=coupon 지급 연동은 이번 범위에서 보류(계획 문서 참고, 쿠폰 발급 로직은 관리자 발급 경로 완성 후 연동)
        }

        return new ChallengeRewardClaimResponse(claimedAt, grantedPoint);
    }

    /** 출석 체크 시 AttendanceService에서 호출한다(출석 트리거만 이번 범위, 계획 문서 4절 참고). */
    public void updateProgressOnAttendance(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<Challenge> attendanceChallenges =
                challengeRepository.findByStatusAndTargetAction("ACTIVE", ATTENDANCE_TARGET_ACTION);

        for (Challenge challenge : attendanceChallenges) {
            ChallengeProgress progress = findProgress(memberId, challenge)
                    .orElseGet(() -> ChallengeProgress.create(member, challenge, LocalDate.now()));
            progress.incrementProgress(challenge.getTargetCount());
            challengeProgressRepository.save(progress);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminChallengeSummaryResponse> getAdminChallenges(String challengeType, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasType = challengeType != null && !challengeType.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        Page<Challenge> result;
        if (hasType && hasStatus) {
            result = challengeRepository.findByStatusAndChallengeType(status, challengeType, pageable);
        } else if (hasStatus) {
            result = challengeRepository.findByStatus(status, pageable);
        } else if (hasType) {
            result = challengeRepository.findByChallengeType(challengeType, pageable);
        } else {
            result = challengeRepository.findAll(pageable);
        }

        List<Challenge> challenges = result.getContent();
        Map<Long, List<ChallengeRewardItemResponse>> rewardsByChallengeId = toRewardItemsByChallengeIds(challenges);
        List<AdminChallengeSummaryResponse> content = challenges.stream()
                .map(challenge -> new AdminChallengeSummaryResponse(
                        challenge.getChallengeId(), challenge.getChallengeName(), challenge.getChallengeType(),
                        challenge.getTargetAction(), challenge.getTargetCount(), challenge.getStatus(),
                        rewardsByChallengeId.getOrDefault(challenge.getChallengeId(), List.of())))
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    // 보상 수정은 미수령 진행도가 있어도 제한 없이 허용한다(현행 유지 정책, claimReward() 주석 참고).
    public Long updateChallenge(Long challengeId, AdminChallengeUpdateRequest request) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
        validateRewardItems(request.rewards());
        challenge.update(request.challengeName(), request.challengeType(), request.targetAction(),
                request.targetCount(), request.status());

        challengeRewardRepository.deleteAll(challengeRewardRepository.findByChallenge_ChallengeId(challengeId));
        for (AdminChallengeRewardItemRequest item : request.rewards()) {
            challengeRewardRepository.save(ChallengeReward.create(
                    challenge, item.rewardType(), item.rewardPoint(), null, item.rewardQuantity()));
        }
        return challengeId;
    }

    private void validateRewardItems(List<AdminChallengeRewardItemRequest> rewards) {
        for (AdminChallengeRewardItemRequest item : rewards) {
            if (item == null
                    || !"POINT".equals(item.rewardType())
                    || item.rewardPoint() == null
                    || item.rewardPoint() <= 0
                    || item.couponId() != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private Map<Long, List<ChallengeRewardItemResponse>> toRewardItemsByChallengeIds(List<Challenge> challenges) {
        if (challenges.isEmpty()) {
            return Map.of();
        }

        List<Long> challengeIds = challenges.stream()
                .map(Challenge::getChallengeId)
                .toList();
        return challengeRewardRepository.findByChallenge_ChallengeIdIn(challengeIds).stream()
                .collect(Collectors.groupingBy(
                        reward -> reward.getChallenge().getChallengeId(),
                        Collectors.mapping(
                                r -> new ChallengeRewardItemResponse(
                                        r.getRewardType(), r.getRewardPoint(), r.getRewardQuantity()),
                                Collectors.toList())));
    }

    private ChallengeResponse toResponse(Challenge challenge, List<ChallengeRewardItemResponse> rewards, ChallengeProgress progress) {
        int currentCount = progress != null ? progress.getCurrentCount() : 0;
        boolean achievedYn = progress != null && Boolean.TRUE.equals(progress.getAchievedYn());
        boolean rewardClaimedYn = progress != null && progress.getRewardClaimedAt() != null;

        return new ChallengeResponse(
                challenge.getChallengeId(), challenge.getChallengeName(), challenge.getChallengeType(),
                challenge.getTargetAction(), challenge.getTargetCount(), currentCount,
                achievedYn, rewardClaimedYn, rewards
        );
    }

    private java.util.Optional<ChallengeProgress> findProgress(Long memberId, Challenge challenge) {
        if ("DAILY".equals(challenge.getChallengeType())) {
            return challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdAndProgressDate(
                    memberId, challenge.getChallengeId(), LocalDate.now());
        }
        return challengeProgressRepository.findFirstByMember_MemberIdAndChallenge_ChallengeId(
                memberId, challenge.getChallengeId());
    }
}
