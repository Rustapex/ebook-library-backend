package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PR51 코드리뷰 대응(2026-07-18) §2.2 — ADMIN/WITHDRAWN/DEACTIVATED 보호와 balanceAfter 스냅샷 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminPointAdjustExecutorTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    @InjectMocks
    private AdminPointAdjustExecutor executor;

    // isAdmin() 단독 검사가 아니라 isProtectedFromAdminChanges()(ADMIN/WITHDRAWN/DEACTIVATED)로
    // 판단하는지가 이번 수정의 핵심이라, Member 판단 로직 자체는 mock으로 고정하고 executor의 반응만 검증한다.
    @Test
    void applyNewAdjustment_보호_대상이면_AUTH_ACCESS_DENIED를_던지고_아무것도_저장하지_않는다() {
        Member member = mock(Member.class);
        given(member.isProtectedFromAdminChanges()).willReturn(true);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> executor.applyNewAdjustment(1L, "req-1", 100, "지급", 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verify(member, never()).applyAdminPointAdjustment(any(Integer.class));
        verify(pointHistoryRepository, never()).save(any());
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void applyNewAdjustment_보호_대상이_아니면_조정_직후_잔액을_balanceAfter로_저장하고_응답한다() {
        Member member = mock(Member.class);
        given(member.isProtectedFromAdminChanges()).willReturn(false);
        given(member.getPointBalance()).willReturn(1100);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        PointHistory saved = mock(PointHistory.class);
        given(saved.getPointHistoryId()).willReturn(5L);
        given(saved.getPointAmount()).willReturn(100);
        given(saved.getPointType()).willReturn("ADMIN_ADJUST");
        given(saved.getBalanceAfter()).willReturn(1100);
        given(saved.getCreatedAt()).willReturn(LocalDateTime.now());
        given(pointHistoryRepository.save(any(PointHistory.class))).willReturn(saved);

        AdminPointAdjustResponse response = executor.applyNewAdjustment(1L, "req-1", 100, "지급", 999L);

        verify(member).applyAdminPointAdjustment(100);
        assertThat(response.pointHistoryId()).isEqualTo(5L);
        assertThat(response.pointBalance()).isEqualTo(1100);
        verify(adminAuditLogWriter).record(eq(999L), any(), any(), eq(1L), any(), any());
    }
}
