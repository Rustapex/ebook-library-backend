package com.acorn.abc.admin.book.controller;

import com.acorn.abc.admin.book.dto.BookIsbnBulkApproveResponse;
import com.acorn.abc.admin.book.dto.BookIsbnTempBulkDeleteResponse;
import com.acorn.abc.batch.service.BookApprovalService;
import com.acorn.abc.batch.service.BookBulkApprovalService;
import com.acorn.abc.batch.service.BookIsbnEnrichService;
import com.acorn.abc.batch.service.BookIsbnSeedService;
import com.acorn.abc.batch.service.BookIsbnTempService;
import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/** 는 SecurityConfig에서 hasRole("ADMIN")으로 보호된다.
 * 실제 SecurityConfig + JwtAuthenticationFilter를 태워 미인증/USER/ADMIN 시나리오를 검증한다.
 * DB 없이 @WebMvcTest 슬라이스로 실행하며, 서비스 계층은 전부 목으로 대체한다.
 */
@WebMvcTest(AdminBookIsbnTempController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        AdminBookSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class AdminBookSecurityIntegrationTest {

    private static final String ENRICH_BODY = """
            {"limit": 2}
            """;

    private static final String BULK_APPROVE_BODY = """
            {
              "limit": 2,
              "maxCandidates": 10,
              "rentalType": "PAID",
              "rentalPrice": 5000,
              "status": "AVAILABLE",
              "dummyPageCount": 3
            }
            """;

    private static final String BULK_DELETE_BODY = """
            {"tempIds": [1, 2]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BookIsbnTempService bookIsbnTempService;
    @MockitoBean
    private BookApprovalService bookApprovalService;
    @MockitoBean
    private BookIsbnSeedService bookIsbnSeedService;
    @MockitoBean
    private BookIsbnEnrichService bookIsbnEnrichService;
    @MockitoBean
    private BookBulkApprovalService bookBulkApprovalService;

    @Test
    void enrich_미인증이면_401과_공통_에러_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENRICH_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void enrich_유효하지_않은_토큰이면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/enrich")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENRICH_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enrich_USER_토큰이면_403과_공통_에러_응답을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "USER");

        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/enrich")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENRICH_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
    }

    @Test
    void bulkApprove_ADMIN_토큰이면_200을_반환하고_memberId를_그대로_전달한다() throws Exception {
        String token = jwtTokenProvider.generateToken(99L, "ADMIN");
        given(bookBulkApprovalService.bulkApprove(any(), eq(99L)))
                .willReturn(new BookIsbnBulkApproveResponse(0, List.of(), List.of()));

        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/bulk-approve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BULK_APPROVE_BODY))
                .andExpect(status().isOk());

        verify(bookBulkApprovalService).bulkApprove(any(), eq(99L));
    }

    @Test
    void bulkDelete_미인증이면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BULK_DELETE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void bulkDelete_USER_토큰이면_403을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "USER");

        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/bulk-delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BULK_DELETE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
    }

    @Test
    void bulkDelete_ADMIN_토큰이면_200을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(99L, "ADMIN");
        given(bookIsbnTempService.bulkDelete(any()))
                .willReturn(new BookIsbnTempBulkDeleteResponse(2, List.of(1L, 2L)));

        mockMvc.perform(post("/api/v1/admin/book-isbn-temps/bulk-delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BULK_DELETE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCount").value(2));

        verify(bookIsbnTempService).bulkDelete(any());
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(
                    "test-secret-key-test-secret-key-test-secret-key-1234567890",
                    3600
            );
        }
    }
}
