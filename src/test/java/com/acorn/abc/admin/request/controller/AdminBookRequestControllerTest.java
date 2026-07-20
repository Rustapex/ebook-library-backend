package com.acorn.abc.admin.request.controller;

import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateResponse;
import com.acorn.abc.admin.request.service.AdminBookRequestService;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBookRequestControllerTest {

    private final AdminBookRequestService adminBookRequestService = mock(AdminBookRequestService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBookRequestController(adminBookRequestService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        CustomUserDetails principal = new CustomUserDetails(99L, "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateStatus_status가_없으면_400을_반환하고_service를_호출하지_않는다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/book-request-candidates/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminBookRequestService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void updateStatus_APPROVED_정상_요청은_200과_처리건수를_반환한다() throws Exception {
        when(adminBookRequestService.updateStatus(eq(99L), eq(1L), any()))
                .thenReturn(new AdminBookRequestStatusUpdateResponse(1L, "APPROVED", 2, 7L, null));

        mockMvc.perform(patch("/api/v1/admin/book-request-candidates/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedBookId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.affectedRequestCount").value(2))
                .andExpect(jsonPath("$.data.approvedBookId").value(7));

        verify(adminBookRequestService).updateStatus(eq(99L), eq(1L), any());
    }

    @Test
    void updateStatus_rejectReason이_501자면_400을_반환한다() throws Exception {
        String tooLongReason = "가".repeat(501);
        mockMvc.perform(patch("/api/v1/admin/book-request-candidates/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectReason\":\"" + tooLongReason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminBookRequestService, never()).updateStatus(any(), any(), any());
    }
}
