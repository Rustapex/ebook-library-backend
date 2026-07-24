package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.LoginResponse;
import com.acorn.abc.auth.service.GoogleAuthService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google 로그인 구현 필수 test 17 — 기존 JWT response 계약 회귀와 상태 구분.
 *
 * <p>AUTHENTICATED 응답의 login 객체는 기존 일반 로그인의 {@link LoginResponse}와 동일한 필드를 가져야 한다.
 * refresh token은 이 저장소에 존재하지 않으므로 추가되지 않았음을 함께 확인한다.
 */
class GoogleAuthControllerTest {

    private static final String LOGIN_BODY = "{\"idToken\":\"google-id-token\"}";

    private final GoogleAuthService googleAuthService = mock(GoogleAuthService.class);

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new GoogleAuthController(googleAuthService))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

    private static LoginResponse loginResponse() {
        return new LoginResponse("issued-access-token", "Bearer", 86400L,
                new LoginResponse.MemberInfo(1L, "googleuser1", "구글사용자", "USER"));
    }

    @Test
    void AUTHENTICATED는_기존_LoginResponse_계약을_그대로_반환한다() throws Exception {
        when(googleAuthService.authenticate(any())).thenReturn(GoogleAuthResponse.authenticated(loginResponse()));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.data.login.accessToken").value("issued-access-token"))
                .andExpect(jsonPath("$.data.login.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.login.expiresIn").value(86400))
                .andExpect(jsonPath("$.data.login.member.memberId").value(1))
                .andExpect(jsonPath("$.data.login.member.loginId").value("googleuser1"))
                .andExpect(jsonPath("$.data.login.member.role").value("USER"))
                // 이 저장소에는 refresh token 계약이 없다. Google 로그인 때문에 추가하지 않는다.
                .andExpect(jsonPath("$.data.login.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.transition").doesNotExist());
    }

    @Test
    void SIGNUP_REQUIRED는_access_token을_포함하지_않는다() throws Exception {
        when(googleAuthService.authenticate(any())).thenReturn(
                GoogleAuthResponse.signupRequired("signup-transition-token", 300L,
                        "google.user@example.com", "구글사용자"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.data.transition.transitionToken").value("signup-transition-token"))
                .andExpect(jsonPath("$.data.transition.expiresIn").value(300));
    }

    @Test
    void LINK_REQUIRED는_access_token을_포함하지_않는다() throws Exception {
        when(googleAuthService.authenticate(any())).thenReturn(
                GoogleAuthResponse.linkRequired("link-transition-token", 300L,
                        "google.user@example.com", "구글사용자"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LINK_REQUIRED"))
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.data.transition.transitionToken").value("link-transition-token"));
    }

    @Test
    void 잘못된_Google_token은_401과_공통_에러_JSON을_반환한다() throws Exception {
        when(googleAuthService.authenticate(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_GOOGLE_TOKEN_INVALID"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void email_verified가_false면_401을_반환한다() throws Exception {
        when(googleAuthService.authenticate(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_GOOGLE_EMAIL_NOT_VERIFIED));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_GOOGLE_EMAIL_NOT_VERIFIED"));
    }

    @Test
    void feature_flag가_꺼져_있으면_503을_반환한다() throws Exception {
        when(googleAuthService.authenticate(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_GOOGLE_DISABLED));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTH_GOOGLE_DISABLED"));
    }

    @Test
    void idToken이_비어있으면_400_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("idToken"));
    }

    @Test
    void transition_token이_만료되면_401을_반환한다() throws Exception {
        when(googleAuthService.completeSignup(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID));

        mockMvc.perform(post("/api/v1/auth/google/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transitionToken\":\"expired\",\"loginId\":\"googleuser1\","
                                + "\"name\":\"구글사용자\",\"password\":\"Passw0rd!!\",\"passwordConfirm\":\"Passw0rd!!\","
                                + "\"phone\":\"010-0714-0001\",\"birthDate\":\"1995-07-14\",\"gender\":\"MALE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TRANSITION_TOKEN_INVALID"));
    }

    @Test
    void 이미_연결된_계정이면_409를_반환한다() throws Exception {
        when(googleAuthService.completeLink(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED));

        mockMvc.perform(post("/api/v1/auth/google/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transitionToken\":\"link-token\",\"password\":\"Passw0rd!!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_OAUTH_ALREADY_LINKED"));
    }

    @Test
    void 비밀번호_재인증_실패는_기존_AUTH_LOGIN_FAILED를_그대로_사용한다() throws Exception {
        when(googleAuthService.completeLink(any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_LOGIN_FAILED));

        mockMvc.perform(post("/api/v1/auth/google/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transitionToken\":\"link-token\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_FAILED"));
    }
}
