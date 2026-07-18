package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 관리자 회원 API 구현계획(2026-07-16) §4.3 — 004 포인트 조정 멱등성/경합 처리 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminPointAdjustServiceTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private AdminPointAdjustExecutor executor;

    private AdminPointAdjustService newService() {
        return new AdminPointAdjustService(pointHistoryRepository, executor);
    }

    private PointHistory existingHistory(int amount, String description) {
        return existingHistory(amount, description, 1100);
    }

    private PointHistory existingHistory(int amount, String description, int balanceAfter) {
        PointHistory history = mock(PointHistory.class);
        given(history.getPointHistoryId()).willReturn(1L);
        given(history.getPointAmount()).willReturn(amount);
        given(history.getPointType()).willReturn("ADMIN_ADJUST");
        given(history.getDescription()).willReturn(description);
        given(history.getCreatedAt()).willReturn(LocalDateTime.now());
        given(history.getBalanceAfter()).willReturn(balanceAfter);
        return history;
    }

    @Test
    void adjustPoint_pointAmount가_0이면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> newService().adjustPoint(1L, "req-1", 0, "사유", 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(executor, never()).applyNewAdjustment(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    void adjustPoint_같은_requestId_같은_payload면_기존_이력을_그대로_반환하고_executor를_호출하지_않는다() {
        PointHistory existing = existingHistory(100, "지급");
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1")).willReturn(Optional.of(existing));

        AdminPointAdjustResponse response = newService().adjustPoint(1L, "req-1", 100, "지급", 999L);

        assertThat(response.pointHistoryId()).isEqualTo(1L);
        assertThat(response.pointAmount()).isEqualTo(100);
        assertThat(response.pointBalance()).isEqualTo(1100);
        verify(executor, never()).applyNewAdjustment(any(), any(), any(Integer.class), any(), any());
    }

    // PR51 코드리뷰 대응(2026-07-18) §2.2-4 회귀 테스트 — 재시도 시점에 회원 잔액이 이미 달라져 있어도
    // 재시도 응답은 조회 시점의 현재 잔액이 아니라 최초 조정 직후 저장된 balanceAfter를 그대로 반환해야 한다.
    @Test
    void adjustPoint_재시도_시점에_잔액이_달라져도_최초_조정_직후_balanceAfter를_반환한다() {
        PointHistory existing = existingHistory(100, "지급", 1100);
        Member currentMember = mock(Member.class);
        given(currentMember.getPointBalance()).willReturn(9999);
        given(existing.getMember()).willReturn(currentMember);
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1")).willReturn(Optional.of(existing));

        AdminPointAdjustResponse response = newService().adjustPoint(1L, "req-1", 100, "지급", 999L);

        assertThat(response.pointBalance()).isEqualTo(1100);
    }

    @Test
    void adjustPoint_같은_requestId_다른_payload면_IDEMPOTENCY_KEY_CONFLICT를_던진다() {
        PointHistory existing = existingHistory(100, "지급");
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> newService().adjustPoint(1L, "req-1", 200, "지급", 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void adjustPoint_신규_요청이면_executor에게_위임한다() {
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1")).willReturn(Optional.empty());
        AdminPointAdjustResponse expected = new AdminPointAdjustResponse(2L, 100, "ADMIN_ADJUST", 1100, LocalDateTime.now());
        given(executor.applyNewAdjustment(1L, "req-1", 100, "지급", 999L)).willReturn(expected);

        AdminPointAdjustResponse response = newService().adjustPoint(1L, "req-1", 100, "지급", 999L);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void adjustPoint_동시경합으로_unique제약위반이_나도_같은_payload면_기존_이력으로_replay한다() {
        PointHistory raced = existingHistory(100, "지급");
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1"))
                .willReturn(Optional.empty(), Optional.of(raced));
        given(executor.applyNewAdjustment(eq(1L), eq("req-1"), any(Integer.class), anyString(), any()))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        AdminPointAdjustResponse response = newService().adjustPoint(1L, "req-1", 100, "지급", 999L);

        assertThat(response.pointHistoryId()).isEqualTo(1L);
    }

    @Test
    void adjustPoint_동시경합_이후_재조회에서도_못찾으면_원래_예외를_던진다() {
        given(pointHistoryRepository.findByMember_MemberIdAndRequestId(1L, "req-1"))
                .willReturn(Optional.empty(), Optional.empty());
        DataIntegrityViolationException original = new DataIntegrityViolationException("unique violation");
        given(executor.applyNewAdjustment(eq(1L), eq("req-1"), any(Integer.class), anyString(), any()))
                .willThrow(original);

        assertThatThrownBy(() -> newService().adjustPoint(1L, "req-1", 100, "지급", 999L))
                .isSameAs(original);
    }
}
