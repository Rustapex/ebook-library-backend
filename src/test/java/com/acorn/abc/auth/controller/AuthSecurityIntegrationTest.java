package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.CheckLoginIdResponse;
import com.acorn.abc.auth.service.AuthService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인아이디 중복확인 API 구현계획(2026-07-15) §4.3.
 * /api/v1/auth/check-login-id는 SecurityConfig에서 GET permitAll이다.
 * 실제 SecurityConfig + JwtAuthenticationFilter를 태워, JWT 없이도 401이 아니라
 * 입력값에 따른 200/400이 반환되는지 확인한다.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        AuthSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void checkLoginId_JWT_없이_유효한_요청이면_200을_반환한다() throws Exception {
        given(authService.checkLoginId(eq("newmember1"))).willReturn(new CheckLoginIdResponse("newmember1", true));

        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", "newmember1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void checkLoginId_JWT_없이_잘못된_요청이면_401이_아니라_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", "가나다"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
