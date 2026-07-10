package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.dto.AdminAuditLogSummaryResponse;
import com.acorn.abc.statistic.service.AdminAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditLogControllerTest {

    private final AdminAuditLogService adminAuditLogService = mock(AdminAuditLogService.class);

    private MockMvc validatedMockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        AdminAuditLogController controller = (AdminAuditLogController) postProcessor.postProcessAfterInitialization(
                new AdminAuditLogController(adminAuditLogService), "adminAuditLogController");

        validatedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAuditLogs_page가_음수이면_400을_반환한다() throws Exception {
        validatedMockMvc.perform(get("/api/v1/admin/audit-logs").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAuditLogs_size가_0이면_400을_반환한다() throws Exception {
        validatedMockMvc.perform(get("/api/v1/admin/audit-logs").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAuditLogs_정상_요청은_200을_반환한다() throws Exception {
        when(adminAuditLogService.getAuditLogs(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.<AdminAuditLogSummaryResponse>of(), 0, 10, 0));

        validatedMockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isOk());
    }
}
