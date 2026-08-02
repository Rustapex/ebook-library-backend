package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 관리자 포인트 조정(API-ADMIN-MEMBER-004)의 신규 요청 처리 전용(구현계획 §1.6.2). 이 클래스가 단일
 * 트랜잭션(잠금 조회 + 잔액 갱신 + 이력 저장 + 감사 로그)을 담당한다. 회원별 유니크 제약 위반 시
 * DataIntegrityViolationException으로 이 트랜잭션 전체가 rollback되어야 하므로, 위반 이후 기존 이력을
 * 다시 읽는 재시도/replay 로직은 이 트랜잭션 밖(AdminPointAdjustService, 별도 빈)에서 처리한다 —
 * 같은 클래스 자기호출로는 새 트랜잭션 경계가 생기지 않기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class AdminPointAdjustExecutor {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional
    public AdminPointAdjustResponse applyNewAdjustment(Long memberId, String requestId, int pointAmount,
                                                         String description, Long adminMemberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // PR51 코드리뷰 대응 §2.2-1 — isAdmin() 단독 검사 대신 ADMIN/WITHDRAWN/DEACTIVATED를 모두 거부한다.
        if (member.isProtectedFromAdminChanges()) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        member.applyAdminPointAdjustment(pointAmount);
        PointHistory history = pointHistoryRepository.save(
                PointHistory.createAdminAdjust(member, pointAmount, description, requestId, member.getPointBalance()));

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.MEMBER_POINT_ADJUST,
                AdminAuditLogTargetType.MEMBER, memberId, null,
                Map.of("pointAmount", pointAmount, "balanceAfter", history.getBalanceAfter(), "requestId", requestId));

        return new AdminPointAdjustResponse(history.getPointHistoryId(), history.getPointAmount(),
                history.getPointType(), history.getBalanceAfter(), history.getCreatedAt());
    }
}
