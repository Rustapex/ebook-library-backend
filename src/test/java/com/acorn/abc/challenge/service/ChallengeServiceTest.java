package com.acorn.abc.challenge.service;

import com.acorn.abc.admin.challenge.dto.AdminChallengeRewardItemRequest;
import com.acorn.abc.admin.challenge.dto.AdminChallengeSummaryResponse;
import com.acorn.abc.admin.challenge.dto.AdminChallengeUpdateRequest;
import com.acorn.abc.challenge.domain.Challenge;
import com.acorn.abc.challenge.domain.ChallengeReward;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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

    private Challenge challenge(Long id, String name) {
        Challenge challenge = mock(Challenge.class);
        given(challenge.getChallengeId()).willReturn(id);
        given(challenge.getChallengeName()).willReturn(name);
        given(challenge.getChallengeType()).willReturn("DAILY");
        given(challenge.getTargetAction()).willReturn("ATTENDANCE");
        given(challenge.getTargetCount()).willReturn(1);
        given(challenge.getStatus()).willReturn("ACTIVE");
        return challenge;
    }
}
