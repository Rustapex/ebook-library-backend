package com.acorn.abc.admin.collection.controller;

import com.acorn.abc.collection.service.AdminCollectionService;
import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/** 는 SecurityConfig에서 hasRole("ADMIN")으로 보호된다(구현계획 §9.1).
 * 이 경로가 그 규칙을 실제로 상속받는지 목록 GET 하나로 확인한다 — 규칙 자체가
 * prefix 단위라 다른 5개 엔드포인트에 동일 테스트를 반복하지 않는다.
 */
@WebMvcTest(AdminCollectionController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        AdminCollectionSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class AdminCollectionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AdminCollectionService adminCollectionService;

    @Test
    void getCollections_미인증이면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/collections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCollections_USER_토큰이면_403을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "USER");

        mockMvc.perform(get("/api/v1/admin/collections").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
    }

    @Test
    void getCollections_ADMIN_토큰이면_200을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(99L, "ADMIN");
        given(adminCollectionService.getAdminCollections(any(), any(), anyInt(), anyInt()))
                .willReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/v1/admin/collections").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
