package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.LoginResponse;
import com.acorn.abc.auth.service.GoogleAuthService;
import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.repository.MemberRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google 로그인 endpoint 3개는 ABC access token 없이 호출되므로 SecurityConfig에서 POST permitAll이다.
 * 실제 SecurityConfig + JwtAuthenticationFilter를 태워 401이 아니라 controller까지 도달하는지 확인한다.
 *
 * <p>permitAll 범위가 넓어지지 않았는지(하위 경로 wildcard 금지)도 함께 확인한다.
 */
@WebMvcTest(GoogleAuthController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        GoogleAuthSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class GoogleAuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    // JwtAuthenticationFilter가 회원 상태를 조회하므로 슬라이스 컨텍스트에도 빈이 필요하다.
    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void google_로그인은_JWT_없이_401이_아니라_200을_반환한다() throws Exception {
        given(googleAuthService.authenticate(any())).willReturn(
                GoogleAuthResponse.authenticated(new LoginResponse("token", "Bearer", 86400L,
                        new LoginResponse.MemberInfo(1L, "googleuser1", "구글사용자", "USER"))));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"));
    }

    @Test
    void google_signup은_JWT_없이_401이_아니라_400_검증오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transitionToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void google_link는_JWT_없이_401이_아니라_400_검증오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transitionToken\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 등록되지_않은_google_하위경로는_permitAll이_아니다() throws Exception {
        // wildcard로 열지 않았음을 확인한다. 인증 없이 접근하면 401이어야 한다.
        mockMvc.perform(post("/api/v1/auth/google/unlink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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
