package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    @InjectMocks
    private PointService pointService;

    @Test
    void adjustPointByAdmin_성공하면_감사로그를_기록한다() {
        Member member = mock(Member.class);
        given(member.getPointBalance()).willReturn(500);
        given(memberRepository.applyPointChangeIfSufficient(1L, 100)).willReturn(1);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        pointService.adjustPointByAdmin(1L, 100, "관리자 지급", 999L);

        verify(adminAuditLogWriter).record(
                eq(999L), eq(AdminAuditLogActionType.MEMBER_POINT_ADJUST),
                eq(AdminAuditLogTargetType.MEMBER), eq(1L), any(), any());
    }

    @Test
    void grantOrAdjustPoint_원자적업데이트가_성공하면_포인트이력을_저장한다() {
        Member member = mock(Member.class);
        given(memberRepository.applyPointChangeIfSufficient(1L, 100)).willReturn(1);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        PointHistory history = pointService.grantOrAdjustPoint(1L, 100, "challenge_reward", "챌린지 보상");

        assertThat(history.getMember()).isSameAs(member);
        assertThat(history.getPointType()).isEqualTo("challenge_reward");
        assertThat(history.getPointAmount()).isEqualTo(100);
        verify(memberRepository).applyPointChangeIfSufficient(1L, 100);
        verify(member, never()).applyPointChange(100);
    }

    @Test
    void grantOrAdjustPoint_잔액부족이면_포인트이력을_저장하지_않는다() {
        given(memberRepository.applyPointChangeIfSufficient(1L, -500)).willReturn(0);
        given(memberRepository.existsById(1L)).willReturn(true);

        assertThatThrownBy(() -> pointService.grantOrAdjustPoint(1L, -500, "ADMIN_ADJUST", "차감"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void grantOrAdjustPoint_회원이_없으면_MEMBER_NOT_FOUND를_던진다() {
        given(memberRepository.applyPointChangeIfSufficient(1L, 100)).willReturn(0);
        given(memberRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> pointService.grantOrAdjustPoint(1L, 100, "challenge_reward", "챌린지 보상"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(pointHistoryRepository, never()).save(any());
    }
}
