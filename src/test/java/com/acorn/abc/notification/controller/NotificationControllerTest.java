package com.acorn.abc.notification.controller;

import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.notification.dto.NotificationItemResponse;
import com.acorn.abc.notification.dto.NotificationReadResponse;
import com.acorn.abc.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        CustomUserDetails principal = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyNotifications_필터와_목록_응답을_반환한다() throws Exception {
        NotificationItemResponse item = new NotificationItemResponse(
                5L, "NOTICE", "공지", "NOTICE", 3L, false, LocalDateTime.now(), null);
        when(notificationService.getMyNotifications(1L, false, "NOTICE", 0, 10))
                .thenReturn(PageResponse.of(List.of(item), 0, 10, 1));

        mockMvc.perform(get("/api/v1/me/notifications")
                        .param("readYn", "false").param("notificationType", "NOTICE")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].notificationId").value(5))
                .andExpect(jsonPath("$.data.content[0].readYn").value(false));

        verify(notificationService).getMyNotifications(1L, false, "NOTICE", 0, 10);
    }

    @Test
    void markAsRead_이미_읽은_알림도_200을_반환한다() throws Exception {
        when(notificationService.markAsRead(1L, 5L))
                .thenReturn(new NotificationReadResponse(5L, true, LocalDateTime.now(), "NOTICE", 3L));

        mockMvc.perform(patch("/api/v1/me/notifications/5/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readYn").value(true));

        verify(notificationService).markAsRead(1L, 5L);
    }
}
