package com.acorn.abc.statistic.service;

import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.domain.AdminAuditLog;
import com.acorn.abc.statistic.dto.AdminAuditLogSummaryResponse;
import com.acorn.abc.statistic.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private AdminAuditLogService adminAuditLogService;

    @Test
    void getAuditLogs_필터와_페이지_정보를_그대로_전달하고_PageResponse로_변환한다() {
        AdminAuditLog log = mock(AdminAuditLog.class);
        given(adminAuditLogRepository.search(eq("BOOK_CREATE"), eq("BOOK"), eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResponse<AdminAuditLogSummaryResponse> response =
                adminAuditLogService.getAuditLogs("BOOK_CREATE", "BOOK", 1L, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
    }
}
