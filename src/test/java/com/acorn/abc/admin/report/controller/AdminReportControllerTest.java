package com.acorn.abc.admin.report.controller;

import com.acorn.abc.admin.report.service.AdminReportService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminReportControllerTest {

    private static final long ADMIN_ID = 1L;

    private final AdminReportService adminReportService = mock(AdminReportService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReportController(adminReportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        CustomUserDetails principal = new CustomUserDetails(ADMIN_ID, "ADMIN");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReports_정상_요청이면_200을_반환한다() throws Exception {
        when(adminReportService.getAdminReports(any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/v1/admin/reports").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void processReport_status가_없으면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reports/BOOK/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void processReport_허용되지_않은_상태전이면_409를_반환한다() throws Exception {
        when(adminReportService.processReport(eq("BOOK"), eq(1L), any(), eq(ADMIN_ID)))
                .thenThrow(new BusinessException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION));

        mockMvc.perform(patch("/api/v1/admin/reports/BOOK/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_INVALID_STATUS_TRANSITION"));
    }
}
