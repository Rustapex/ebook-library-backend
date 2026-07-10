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

    private final AdminAuditLogRepository adminAuditLogRepository;

    public PageResponse<AdminAuditLogSummaryResponse> getAuditLogs(
            String actionType, String targetType, Long targetId, int page, int size) {
        Page<AdminAuditLogSummaryResponse> result = adminAuditLogRepository
                .search(actionType, targetType, targetId, PageRequest.of(page, size))
                .map(AdminAuditLogSummaryResponse::from);
        return PageResponse.of(result.getContent(), page, size, result.getTotalElements());
    }
}
