package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.review.domain.SanctionHistory;
import com.acorn.abc.review.repository.SanctionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 계정 정지 만료 복구 배치(구현계획 §1.5). 이 프로젝트에는 아직 스케줄러 인프라(@EnableScheduling 등)가
 * 없고 계획도 새 endpoint 추가를 금지하므로, 이 메서드는 향후 스케줄러/운영 트리거에서 그대로 재사용할 수
 * 있는 형태로만 제공한다 — 현재는 테스트에서 직접 호출해 검증한다.
 *
 * 후보 처리 자체는 AdminMemberService.restoreAccountSuspensionIfEligible()(별도 빈, 후보 1건당 @Transactional)에
 * 위임한다. 이 클래스에는 @Transactional을 붙이지 않아 후보 하나의 실패/잠금대기가 나머지 후보 처리를 막지 않는다
 * (BookBulkApprovalService와 동일한 패턴).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSuspensionExpiryBatchService {

    private final SanctionHistoryRepository sanctionHistoryRepository;
    private final AdminMemberService adminMemberService;

    public int restoreExpiredAccountSuspensions() {
        LocalDateTime now = LocalDateTime.now();
        List<SanctionHistory> candidates = sanctionHistoryRepository.findExpiredAccountSuspensionCandidates(now);

        int restoredCount = 0;
        for (SanctionHistory candidate : candidates) {
            try {
                if (adminMemberService.restoreAccountSuspensionIfEligible(candidate.getSanctionHistoryId(), now)) {
                    restoredCount++;
                }
            } catch (BusinessException e) {
                log.warn("계정 정지 만료 복구 실패(비즈니스 예외) sanctionHistoryId={}, code={}",
                        candidate.getSanctionHistoryId(), e.getErrorCode().getCode());
            } catch (PessimisticLockingFailureException e) {
                log.warn("계정 정지 만료 복구 실패(잠금 대기 초과) sanctionHistoryId={}", candidate.getSanctionHistoryId());
            }
        }
        return restoredCount;
    }
}
