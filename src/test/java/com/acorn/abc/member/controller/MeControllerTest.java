package com.acorn.abc.member.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeControllerTest {

    private static final long MEMBER_ID = 1L;

    private final MemberService memberService = mock(MemberService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeController(memberService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        CustomUserDetails principal = new CustomUserDetails(MEMBER_ID, "USER");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMe_로그인한_회원의_기존_정보를_반환한다() throws Exception {
        when(memberService.getMe(MEMBER_ID)).thenReturn(existingInfoResponse());

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("member0714a"))
                .andExpect(jsonPath("$.data.email").value("member0714a@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("JOINED"));
    }

    @Test
    void updateMe_정상_요청이면_수정된_정보를_반환한다() throws Exception {
        when(memberService.updateMyInfo(eq(MEMBER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(updatedInfoResponse());

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"회원A수정\",\"email\":\"updated@example.com\",\"phone\":\"010-0714-9999\",\"gender\":\"FEMALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("회원A수정"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));
    }

    @Test
    void updateMe_이메일_형식이_올바르지_않으면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMe_gender가_허용값이_아니면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"OTHER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMe_이메일이_중복이면_409와_MEMBER_EMAIL_DUPLICATED를_반환한다() throws Exception {
        when(memberService.updateMyInfo(eq(MEMBER_ID), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicated@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_EMAIL_DUPLICATED"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    private MeInfoResponse existingInfoResponse() {
        return new MeInfoResponse(
                MEMBER_ID, "member0714a", "회원A", "member0714a@example.com", "010-0714-0001",
                LocalDate.of(1995, 7, 14), "MALE", "USER", "JOINED");
    }

    private MeInfoResponse updatedInfoResponse() {
        return new MeInfoResponse(
                MEMBER_ID, "member0714a", "회원A수정", "updated@example.com", "010-0714-9999",
                LocalDate.of(1995, 7, 14), "FEMALE", "USER", "JOINED");
    }
}
