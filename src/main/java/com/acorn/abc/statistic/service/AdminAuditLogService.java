package com.acorn.abc.statistic.service;

import com.acorn.abc.common.response.PageResponse;
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
        int normalizedSize = Math.min(size, MAX_SIZE);
        Page<AdminAuditLogSummaryResponse> result = adminAuditLogRepository
                .search(actionType, targetType, targetId, PageRequest.of(page, normalizedSize))
                .map(AdminAuditLogSummaryResponse::from);
        return PageResponse.of(result.getContent(), page, normalizedSize, result.getTotalElements());
    }
}
