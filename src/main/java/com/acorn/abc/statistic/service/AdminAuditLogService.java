package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.dto.AdminAuditLogSummaryResponse;
import com.acorn.abc.statistic.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditLogService {

    // BookService/CollectionService와 동일한 패턴(상한은 @Max 검증이 아니라 조용히 클램프).
    private static final int MAX_SIZE = 100;

    private final AdminAuditLogRepository adminAuditLogRepository;

    public PageResponse<AdminAuditLogSummaryResponse> getAuditLogs(
            String actionType, String targetType, Long targetId, int page, int size) {
        // 관리자API+사용자통계 전체잔여작업 구현계획 §3.3 안 A: actionType/targetType을 enum으로 파싱한다.
        // 알려진 값 목록에 없으면 조용히 빈 목록을 반환하지 않고 fail-fast한다(통계 API periodType/ageBand와 동일 정책).
        AdminAuditLogActionType actionTypeEnum = parseActionType(actionType);
        AdminAuditLogTargetType targetTypeEnum = parseTargetType(targetType);

        int normalizedSize = Math.min(size, MAX_SIZE);
        Page<AdminAuditLogSummaryResponse> result = adminAuditLogRepository
                .search(actionTypeEnum, targetTypeEnum, targetId, PageRequest.of(page, normalizedSize))
                .map(AdminAuditLogSummaryResponse::from);
        return PageResponse.of(result.getContent(), page, normalizedSize, result.getTotalElements());
    }

    private AdminAuditLogActionType parseActionType(String actionType) {
        if (actionType == null) {
            return null;
        }
        try {
            return AdminAuditLogActionType.valueOf(actionType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private AdminAuditLogTargetType parseTargetType(String targetType) {
        if (targetType == null) {
            return null;
        }
        try {
            return AdminAuditLogTargetType.valueOf(targetType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
