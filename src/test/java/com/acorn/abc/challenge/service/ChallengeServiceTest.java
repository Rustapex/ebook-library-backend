package com.acorn.abc.challenge.service;

import com.acorn.abc.admin.challenge.dto.AdminChallengeRewardItemRequest;
import com.acorn.abc.admin.challenge.dto.AdminChallengeSummaryResponse;
import com.acorn.abc.admin.challenge.dto.AdminChallengeUpdateRequest;
import com.acorn.abc.challenge.domain.Challenge;
import com.acorn.abc.challenge.domain.ChallengeProgress;
import com.acorn.abc.challenge.domain.ChallengeReward;
import com.acorn.abc.challenge.dto.ChallengeResponse;
import com.acorn.abc.challenge.dto.ChallengeRewardClaimResponse;
import com.acorn.abc.challenge.repository.ChallengeProgressRepository;
import com.acorn.abc.challenge.repository.ChallengeRepository;
import com.acorn.abc.challenge.repository.ChallengeRewardRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.service.PointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeProgressRepository challengeProgressRepository;

    @Mock
    private ChallengeRewardRepository challengeRewardRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointService pointService;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void updateChallenge_rewardType_coupon이면_등록을_차단한다() {
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "ACTIVE",
                List.of(new AdminChallengeRewardItemRequest("coupon", null, 10L, 1)));

        assertThatThrownBy(() -> challengeService.updateChallenge(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(challenge, never()).update(any(), any(), any(), anyInt(), any());
        verify(challengeRewardRepository, never()).deleteAll(any());
    }

    @Test
    void getAdminChallenges_보상목록을_배치조회해서_매핑한다() {
        Challenge challenge1 = challenge(1L, "매일 출석");
        Challenge challenge2 = challenge(2L, "누적 출석");
        ChallengeReward reward = mock(ChallengeReward.class);
        given(reward.getChallenge()).willReturn(challenge1);
        given(reward.getRewardType()).willReturn("POINT");
        given(reward.getRewardPoint()).willReturn(100);
        given(reward.getRewardQuantity()).willReturn(1);
        given(challengeRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(challenge1, challenge2)));
        given(challengeRewardRepository.findByChallenge_ChallengeIdIn(List.of(1L, 2L)))
                .willReturn(List.of(reward));

        PageResponse<AdminChallengeSummaryResponse> response =
                challengeService.getAdminChallenges(null, null, 0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).rewards()).hasSize(1);
        assertThat(response.content().get(1).rewards()).isEmpty();
        verify(challengeRewardRepository).findByChallenge_ChallengeIdIn(List.of(1L, 2L));
        verify(challengeRewardRepository, never()).findByChallenge_ChallengeId(anyLong());
    }

    @Test
    void updateChallenge_rewardPoint가_0이면_등록을_차단한다() {
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "ACTIVE",
                List.of(new AdminChallengeRewardItemRequest("POINT", 0, null, 1)));

        assertThatThrownBy(() -> challengeService.updateChallenge(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(challenge, never()).update(any(), any(), any(), anyInt(), any());
        verify(challengeRewardRepository, never()).deleteAll(any());
    }

    @Test
    void updateChallenge_rewardPoint가_음수이면_등록을_차단한다() {
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "ACTIVE",
                List.of(new AdminChallengeRewardItemRequest("POINT", -100, null, 1)));

        assertThatThrownBy(() -> challengeService.updateChallenge(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(challenge, never()).update(any(), any(), any(), anyInt(), any());
    }

    @Test
    void getChallenges_진행도를_DAILY_TOTAL_그룹별로_배치조회해서_매핑한다() {
        Challenge dailyChallenge = challenge(1L, "매일 출석", "DAILY");
        Challenge totalChallenge = challenge(2L, "누적 출석", "TOTAL");
        ChallengeProgress dailyProgress = mock(ChallengeProgress.class);
        given(dailyProgress.getChallenge()).willReturn(dailyChallenge);
        given(dailyProgress.getCurrentCount()).willReturn(1);
        given(dailyProgress.getAchievedYn()).willReturn(true);
        ChallengeProgress totalProgress = mock(ChallengeProgress.class);
        given(totalProgress.getChallenge()).willReturn(totalChallenge);
        given(totalProgress.getCurrentCount()).willReturn(10);
        given(totalProgress.getAchievedYn()).willReturn(false);

        given(challengeRepository.findByStatus("ACTIVE")).willReturn(List.of(dailyChallenge, totalChallenge));
        given(challengeRewardRepository.findByChallenge_ChallengeIdIn(List.of(1L, 2L))).willReturn(List.of());
        given(challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
                eq(100L), eq(List.of(1L)), any())).willReturn(List.of(dailyProgress));
        given(challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdIn(100L, List.of(2L)))
                .willReturn(List.of(totalProgress));

        List<ChallengeResponse> responses = challengeService.getChallenges(100L, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).currentCount()).isEqualTo(1);
        assertThat(responses.get(0).achievedYn()).isTrue();
        assertThat(responses.get(1).currentCount()).isEqualTo(10);
        assertThat(responses.get(1).achievedYn()).isFalse();
        verify(challengeProgressRepository).findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
                eq(100L), eq(List.of(1L)), any());
        verify(challengeProgressRepository).findByMember_MemberIdAndChallenge_ChallengeIdIn(100L, List.of(2L));
        verify(challengeProgressRepository, never())
                .findByMember_MemberIdAndChallenge_ChallengeIdAndProgressDate(anyLong(), anyLong(), any());
        verify(challengeProgressRepository, never())
                .findFirstByMember_MemberIdAndChallenge_ChallengeId(anyLong(), anyLong());
    }

    @Test
    void getChallenges_한쪽_타입만_있으면_다른쪽_batch_쿼리를_호출하지_않는다() {
        Challenge dailyChallenge = challenge(1L, "매일 출석", "DAILY");
        given(challengeRepository.findByStatus("ACTIVE")).willReturn(List.of(dailyChallenge));
        given(challengeRewardRepository.findByChallenge_ChallengeIdIn(List.of(1L))).willReturn(List.of());
        given(challengeProgressRepository.findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
                eq(100L), eq(List.of(1L)), any())).willReturn(List.of());

        challengeService.getChallenges(100L, null);

        verify(challengeProgressRepository, never())
                .findByMember_MemberIdAndChallenge_ChallengeIdIn(anyLong(), any());
    }

    @Test
    void claimReward_보상은_달성시점이_아닌_수령시점_최신값을_지급한다() {
        Challenge challenge = mock(Challenge.class);
        given(challenge.getChallengeId()).willReturn(1L);
        given(challenge.getChallengeName()).willReturn("누적 출석");
        given(challenge.getChallengeType()).willReturn("TOTAL");
        ChallengeProgress progress = mock(ChallengeProgress.class);
        given(progress.getChallengeProgressId()).willReturn(10L);
        given(progress.getAchievedYn()).willReturn(true);
        given(progress.getRewardClaimedAt()).willReturn(null);

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(challengeProgressRepository.findFirstByMember_MemberIdAndChallenge_ChallengeId(100L, 1L))
                .willReturn(Optional.of(progress));
        given(challengeProgressRepository.markRewardClaimedIfUnclaimed(eq(10L), any())).willReturn(1);

        // 회원이 달성한 시점엔 보상이 100이었을 수 있지만, 관리자가 그 사이 보상을 200으로 바꿨다고 가정.
        // claimReward()는 달성 시점 값이 아니라 수령 시점(호출 시점)에 조회한 최신 보상을 지급해야 한다.
        ChallengeReward latestReward = mock(ChallengeReward.class);
        given(latestReward.getRewardType()).willReturn("POINT");
        given(latestReward.getRewardPoint()).willReturn(200);
        given(latestReward.getRewardQuantity()).willReturn(1);
        given(challengeRewardRepository.findByChallenge_ChallengeId(1L)).willReturn(List.of(latestReward));

        ChallengeRewardClaimResponse response = challengeService.claimReward(100L, 1L);

        assertThat(response.grantedPoint()).isEqualTo(200);
        verify(challengeRewardRepository).findByChallenge_ChallengeId(1L);
    }

    @Test
    void claimReward_POINT가_아닌_보상은_fail_fast로_막는다() {
        Challenge challenge = mock(Challenge.class);
        given(challenge.getChallengeId()).willReturn(1L);
        ChallengeProgress progress = mock(ChallengeProgress.class);
        given(progress.getChallengeProgressId()).willReturn(10L);
        given(progress.getAchievedYn()).willReturn(true);
        given(progress.getRewardClaimedAt()).willReturn(null);

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(challengeProgressRepository.findFirstByMember_MemberIdAndChallenge_ChallengeId(100L, 1L))
                .willReturn(Optional.of(progress));
        given(challengeProgressRepository.markRewardClaimedIfUnclaimed(eq(10L), any())).willReturn(1);

        ChallengeReward couponReward = mock(ChallengeReward.class);
        given(couponReward.getRewardType()).willReturn("coupon");
        given(challengeRewardRepository.findByChallenge_ChallengeId(1L)).willReturn(List.of(couponReward));

        assertThatThrownBy(() -> challengeService.claimReward(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHALLENGE_REWARD_TYPE_NOT_SUPPORTED);
    }

    private Challenge challenge(Long id, String name) {
        return challenge(id, name, "DAILY");
    }

    private Challenge challenge(Long id, String name, String challengeType) {
        Challenge challenge = mock(Challenge.class);
        given(challenge.getChallengeId()).willReturn(id);
        given(challenge.getChallengeName()).willReturn(name);
        given(challenge.getChallengeType()).willReturn(challengeType);
        given(challenge.getTargetAction()).willReturn("ATTENDANCE");
        given(challenge.getTargetCount()).willReturn(1);
        // getStatus()는 getAdminChallenges() 흐름에서만 쓰인다(ChallengeService.java:166).
        // getChallenges() 흐름 테스트에서는 미사용이라 strict stubbing 예외를 피하려고 lenient 처리한다.
        lenient().when(challenge.getStatus()).thenReturn("ACTIVE");
        return challenge;
    }
}
