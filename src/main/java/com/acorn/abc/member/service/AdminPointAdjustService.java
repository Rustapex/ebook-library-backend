package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * 관리자 포인트 조정(API-ADMIN-MEMBER-004) 진입점 — 멱등 키 조회/replay/충돌 판정을 담당하고,
 * 신규 조정의 실제 잠금·저장은 AdminPointAdjustExecutor(별도 빈, @Transactional)에 위임한다.
 * 이 클래스 자체는 @Transactional이 아니다 — 유니크 제약 위반으로 executor의 트랜잭션이
 * rollback된 뒤에도 그 사실과 무관하게 새 읽기 조회를 수행해야 하기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class AdminPointAdjustService {

    private final PointHistoryRepository pointHistoryRepository;
    private final AdminPointAdjustExecutor executor;

    public AdminPointAdjustResponse adjustPoint(Long memberId, String requestId, int pointAmount,
                                                 String description, Long adminMemberId) {
        if (pointAmount == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalizedDescription = normalize(description);

        Optional<PointHistory> existing = pointHistoryRepository.findByMember_MemberIdAndRequestId(memberId, requestId);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), pointAmount, normalizedDescription);
        }

        try {
            return executor.applyNewAdjustment(memberId, requestId, pointAmount, normalizedDescription, adminMemberId);
        } catch (DataIntegrityViolationException e) {
            // (member_id, request_id) UNIQUE 경합 — 동시에 같은 요청이 먼저 커밋된 경우(§1.6.2-4).
            PointHistory raced = pointHistoryRepository.findByMember_MemberIdAndRequestId(memberId, requestId)
                    .orElseThrow(() -> e);
            return replayOrConflict(raced, pointAmount, normalizedDescription);
        }
    }

    private AdminPointAdjustResponse replayOrConflict(PointHistory existing, int pointAmount, String normalizedDescription) {
        boolean samePayload = existing.getPointAmount() == pointAmount
                && Objects.equals(normalize(existing.getDescription()), normalizedDescription);
        if (!samePayload) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        // PR51 코드리뷰 대응 §2.2-4 — 재시도 응답은 현재 회원 잔액이 아니라 최초 조정 직후 저장한
        // balanceAfter를 그대로 반환한다. 조회 시점 사이에 다른 포인트 변경이 있어도 값이 달라지지 않는다.
        return new AdminPointAdjustResponse(existing.getPointHistoryId(), existing.getPointAmount(),
                existing.getPointType(), existing.getBalanceAfter(), existing.getCreatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.strip();
    }
}
