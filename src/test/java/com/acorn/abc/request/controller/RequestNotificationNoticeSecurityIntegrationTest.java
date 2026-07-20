package com.acorn.abc.request.controller;

import com.acorn.abc.admin.notification.controller.AdminNoticeController;
import com.acorn.abc.admin.notification.dto.AdminNoticeCreateResponse;
import com.acorn.abc.admin.request.controller.AdminBookRequestController;
import com.acorn.abc.admin.request.service.AdminBookRequestService;
import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.notification.controller.NoticeController;
import com.acorn.abc.notification.controller.NotificationController;
import com.acorn.abc.notification.dto.NoticeDetailResponse;
import com.acorn.abc.notification.service.AdminNoticeService;
import com.acorn.abc.notification.service.NoticeService;
import com.acorn.abc.notification.service.NotificationService;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.service.BookRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BookRequestController.class, MyBookRequestController.class, NotificationController.class,
        NoticeController.class, AdminBookRequestController.class, AdminNoticeController.class})
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        RequestNotificationNoticeSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
class RequestNotificationNoticeSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BookRequestService bookRequestService;
    @MockitoBean
    private NoticeService noticeService;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private AdminBookRequestService adminBookRequestService;
    @MockitoBean
    private AdminNoticeService adminNoticeService;

    @Test
    void notices_공개_GET은_JWT_없이_200을_반환한다() throws Exception {
        given(noticeService.getNotices(null, null)).willReturn(PageResponse.of(List.of(), 0, 10, 0));
        given(noticeService.getNoticeDetail(1L))
                .willReturn(new NoticeDetailResponse(1L, "공지", "내용", LocalDateTime.now(), null, null));

        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").value(1));
    }

    @Test
    void bookRequest_미인증_POST는_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/book-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"도서\",\"author\":\"저자\",\"publisher\":\"출판사\",\"reason\":\"사유\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void me_희망도서와_알림_GET은_미인증이면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/book-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
        mockMvc.perform(get("/api/v1/me/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void admin_API는_USER_JWT에_403을_반환한다() throws Exception {
        String userToken = jwtTokenProvider.generateToken(1L, "USER");

        mockMvc.perform(get("/api/v1/admin/book-request-candidates")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\",\"status\":\"ACTIVE\",\"notifyYn\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_JWT는_관리자_API에_접근할_수_있다() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(99L, "ADMIN");
        given(adminBookRequestService.getCandidates(eq(null), eq(null), eq(null), eq(null)))
                .willReturn(PageResponse.of(List.of(), 0, 10, 0));
        LocalDateTime now = LocalDateTime.now();
        given(adminNoticeService.createNotice(eq(99L), any()))
                .willReturn(new AdminNoticeCreateResponse(1L, "공지", "내용", "ACTIVE", now, now, 0));

        mockMvc.perform(get("/api/v1/admin/book-request-candidates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\",\"status\":\"ACTIVE\",\"notifyYn\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.noticeId").value(1));
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider("test-secret-key-test-secret-key-test-secret-key-1234567890", 3600);
        }
    }
}
