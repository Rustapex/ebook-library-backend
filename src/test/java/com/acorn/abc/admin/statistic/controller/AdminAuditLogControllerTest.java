package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.dto.AdminAuditLogSummaryResponse;
import com.acorn.abc.statistic.service.AdminAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    // 관리자API후속작업 구현계획 §7 안 B: 서비스가 던지는 VALIDATION_ERROR가
    // GlobalExceptionHandler를 거쳐 실제로 400까지 이어지는지 확인한다(서비스 단위 테스트와는 별도 경로).
    @Test
    void getAuditLogs_잘못된_actionType이면_400을_반환한다() throws Exception {
        when(adminAuditLogService.getAuditLogs(eq("GARBAGE"), any(), any(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR));

        validatedMockMvc.perform(get("/api/v1/admin/audit-logs").param("actionType", "GARBAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getAuditLogs_잘못된_targetType이면_400을_반환한다() throws Exception {
        when(adminAuditLogService.getAuditLogs(any(), eq("GARBAGE"), any(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR));

        validatedMockMvc.perform(get("/api/v1/admin/audit-logs").param("targetType", "GARBAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // PR #46 리뷰 반영: Swagger @Schema(allowableValues)는 enum과 별도로 문자열 배열로 선언돼 있어
    // 수동 동기화가 필요하다. 두 목록이 어긋나면 이 테스트가 실패로 잡는다.
    @Test
    void getAuditLogs_Schema_allowableValues는_실제_enum_값과_일치한다() throws Exception {
        Method method = AdminAuditLogController.class.getMethod(
                "getAuditLogs", String.class, String.class, Long.class, int.class, int.class);
        java.lang.reflect.Parameter[] parameters = method.getParameters();

        String[] actionTypeAllowableValues = parameters[0]
                .getAnnotation(io.swagger.v3.oas.annotations.Parameter.class).schema().allowableValues();
        String[] targetTypeAllowableValues = parameters[1]
                .getAnnotation(io.swagger.v3.oas.annotations.Parameter.class).schema().allowableValues();

        assertThat(actionTypeAllowableValues).containsExactlyInAnyOrder(
                Arrays.stream(AdminAuditLogActionType.values()).map(Enum::name).toArray(String[]::new));
        assertThat(targetTypeAllowableValues).containsExactlyInAnyOrder(
                Arrays.stream(AdminAuditLogTargetType.values()).map(Enum::name).toArray(String[]::new));
    }
}
