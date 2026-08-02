package com.acorn.abc.admin.notification.controller;

import com.acorn.abc.admin.notification.dto.AdminNoticeCreateResponse;
import com.acorn.abc.admin.notification.dto.AdminNoticeResponse;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.notification.service.AdminNoticeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNoticeControllerTest {

    private final AdminNoticeService adminNoticeService = mock(AdminNoticeService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminNoticeController(adminNoticeService))
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
    void createNotice_정상_요청은_201과_notifyCreatedCount를_반환한다() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(adminNoticeService.createNotice(eq(99L), any()))
                .thenReturn(new AdminNoticeCreateResponse(1L, "공지", "내용", "ACTIVE", now, now, 3));

        mockMvc.perform(post("/api/v1/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\",\"status\":\"ACTIVE\",\"notifyYn\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.notifyCreatedCount").value(3));

        verify(adminNoticeService).createNotice(eq(99L), any());
    }

    @Test
    void createNotice_notifyYn이_없으면_400을_반환하고_service를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminNoticeService, never()).createNotice(any(), any());
    }

    @Test
    void updateNotice_응답에는_notifyCreatedCount를_포함하지_않는다() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(adminNoticeService.updateNotice(eq(99L), eq(1L), any()))
                .thenReturn(new AdminNoticeResponse(1L, "수정 공지", "내용", "ACTIVE", now, now));

        mockMvc.perform(put("/api/v1/admin/notices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정 공지\",\"content\":\"내용\",\"status\":\"ACTIVE\",\"notifyYn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").value(1))
                .andExpect(jsonPath("$.data.notifyCreatedCount").doesNotExist());
    }
}
