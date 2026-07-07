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
import com.acorn.abc.coupon.repository.CouponRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.service.PointService;
import com.acorn.abc.payment.domain.Coupon;
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
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallenges(Long memberId, String challengeType) {
        List<Challenge> challenges = (challengeType == null || challengeType.isBlank())
                ? challengeRepository.findByStatus("ACTIVE")
                : challengeRepository.findByStatusAndChallengeType("ACTIVE", challengeType);

        return challenges.stream()
                .map(challenge -> toResponse(memberId, challenge))
                .toList();
    }

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

        List<AdminChallengeSummaryResponse> content = result.getContent().stream()
                .map(challenge -> new AdminChallengeSummaryResponse(
                        challenge.getChallengeId(), challenge.getChallengeName(), challenge.getChallengeType(),
                        challenge.getTargetAction(), challenge.getTargetCount(), challenge.getStatus(),
                        toRewardItems(challenge.getChallengeId())))
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    public Long updateChallenge(Long challengeId, AdminChallengeUpdateRequest request) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
        challenge.update(request.challengeName(), request.challengeType(), request.targetAction(),
                request.targetCount(), request.status());

        challengeRewardRepository.deleteAll(challengeRewardRepository.findByChallenge_ChallengeId(challengeId));
        for (AdminChallengeRewardItemRequest item : request.rewards()) {
            if ("POINT".equals(item.rewardType()) && item.rewardPoint() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if ("coupon".equals(item.rewardType()) && item.couponId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            Coupon coupon = item.couponId() != null
                    ? couponRepository.findById(item.couponId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND))
                    : null;
            challengeRewardRepository.save(ChallengeReward.create(
                    challenge, item.rewardType(), item.rewardPoint(), coupon, item.rewardQuantity()));
        }
        return challengeId;
    }

    private List<ChallengeRewardItemResponse> toRewardItems(Long challengeId) {
        return challengeRewardRepository.findByChallenge_ChallengeId(challengeId).stream()
                .map(r -> new ChallengeRewardItemResponse(r.getRewardType(), r.getRewardPoint(), r.getRewardQuantity()))
                .toList();
    }

    private ChallengeResponse toResponse(Long memberId, Challenge challenge) {
        ChallengeProgress progress = findProgress(memberId, challenge).orElse(null);
        int currentCount = progress != null ? progress.getCurrentCount() : 0;
        boolean achievedYn = progress != null && Boolean.TRUE.equals(progress.getAchievedYn());
        boolean rewardClaimedYn = progress != null && progress.getRewardClaimedAt() != null;

        List<ChallengeRewardItemResponse> rewards = challengeRewardRepository
                .findByChallenge_ChallengeId(challenge.getChallengeId())
                .stream()
                .map(r -> new ChallengeRewardItemResponse(r.getRewardType(), r.getRewardPoint(), r.getRewardQuantity()))
                .toList();

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
