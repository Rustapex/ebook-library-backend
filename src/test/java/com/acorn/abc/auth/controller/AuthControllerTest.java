package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.service.AuthService;
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
}
