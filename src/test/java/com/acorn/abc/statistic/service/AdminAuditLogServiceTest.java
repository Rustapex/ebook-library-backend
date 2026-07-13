package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void getAuditLogs_size가_100을_넘으면_100으로_클램프한다() {
        given(adminAuditLogRepository.search(any(), any(), any(), eq(PageRequest.of(0, 100))))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        PageResponse<AdminAuditLogSummaryResponse> response =
                adminAuditLogService.getAuditLogs(null, null, null, 0, 500);

        assertThat(response.size()).isEqualTo(100);
    }

    // 관리자API후속작업 구현계획 §7 안 B: 잘못된 actionType/targetType이 조용히 빈 목록으로
    // 처리되지 않고 fail-fast하는지 확인한다(통계 API periodType/ageBand와 동일 정책).
    @Test
    void getAuditLogs_잘못된_actionType은_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminAuditLogService.getAuditLogs("GARBAGE", null, null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(adminAuditLogRepository);
    }

    @Test
    void getAuditLogs_잘못된_targetType은_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminAuditLogService.getAuditLogs(null, "GARBAGE", null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(adminAuditLogRepository);
    }
}
