package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.CheckLoginIdResponse;
import com.acorn.abc.auth.service.AuthService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String SIGNUP_REQUEST_BODY = "{"
            + "\"loginId\":\"member0714a\","
            + "\"password\":\"Passw0rd!!\","
            + "\"passwordConfirm\":\"Passw0rd!!\","
            + "\"name\":\"회원A\","
            + "\"email\":\"member0714a@example.com\","
            + "\"phone\":\"010-0714-0001\","
            + "\"birthDate\":\"1995-07-14\","
            + "\"gender\":\"MALE\"}";

    private final AuthService authService = mock(AuthService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void signup_아이디가_이미_존재하면_409와_공통_에러_JSON을_반환한다() throws Exception {
        when(authService.signup(any())).thenThrow(new BusinessException(ErrorCode.MEMBER_LOGIN_ID_DUPLICATED));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_LOGIN_ID_DUPLICATED"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void signup_이메일이_이미_존재하면_409와_공통_에러_JSON을_반환한다() throws Exception {
        when(authService.signup(any())).thenThrow(new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_EMAIL_DUPLICATED"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    // 로그인아이디 중복확인 API 구현계획(2026-07-15) §4.2.
    @Test
    void checkLoginId_사용_가능한_아이디는_200과_available_true를_반환한다() throws Exception {
        when(authService.checkLoginId("newmember1")).thenReturn(new CheckLoginIdResponse("newmember1", true));

        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", "newmember1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.loginId").value("newmember1"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void checkLoginId_중복된_아이디는_200과_available_false를_반환한다() throws Exception {
        when(authService.checkLoginId("member0714a")).thenReturn(new CheckLoginIdResponse("member0714a", false));

        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", "member0714a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void checkLoginId_loginId가_누락되면_400과_errors에_loginId가_포함된다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-login-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("loginId"));

        verify(authService, never()).checkLoginId(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abcd", "abcdefghijklmnopqrstu", "가나다라마", "member_1", "member-1"})
    void checkLoginId_잘못된_형식이면_400과_VALIDATION_ERROR를_반환한다(String invalidLoginId) throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", invalidLoginId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authService, never()).checkLoginId(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcde", "abcdefghij1234567890"})
    void checkLoginId_5자와_20자_경계값은_검증을_통과한다(String boundaryLoginId) throws Exception {
        when(authService.checkLoginId(boundaryLoginId)).thenReturn(new CheckLoginIdResponse(boundaryLoginId, true));

        mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", boundaryLoginId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }
}
