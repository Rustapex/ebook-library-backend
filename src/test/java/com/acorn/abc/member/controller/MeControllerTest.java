package com.acorn.abc.member.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberGradeSummaryResponse;
import com.acorn.abc.member.service.MeInfoService;
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

import static org.mockito.ArgumentMatchers.any;
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
    private final MeInfoService meInfoService = mock(MeInfoService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeController(memberService, meInfoService))
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
    void getMe_로그인한_회원의_기존_정보와_마이페이지_요약을_반환한다() throws Exception {
        when(meInfoService.getMe(MEMBER_ID)).thenReturn(fullInfoResponse());

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("member0714a"))
                .andExpect(jsonPath("$.data.email").value("member0714a@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("JOINED"))
                .andExpect(jsonPath("$.data.grade.gradeName").value("씨앗"))
                .andExpect(jsonPath("$.data.grade.nextGradeName").value("새싹"))
                .andExpect(jsonPath("$.data.grade.progressRate").value(50.0))
                .andExpect(jsonPath("$.data.pointBalance").value(3000))
                .andExpect(jsonPath("$.data.couponCount").value(2))
                .andExpect(jsonPath("$.data.rentalCount").value(5))
                .andExpect(jsonPath("$.data.completedBookCount").value(3))
                .andExpect(jsonPath("$.data.favoriteCount").value(4))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(1));
    }

    @Test
    void updateMe_정상_요청이면_수정된_정보를_반환한다() throws Exception {
        when(memberService.updateMyInfo(eq(MEMBER_ID), any())).thenReturn(fullInfoResponse());

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"회원A수정\",\"email\":\"updated@example.com\",\"phone\":\"010-0714-9999\",\"gender\":\"FEMALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("회원A"))
                .andExpect(jsonPath("$.data.pointBalance").value(3000));
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
    void updateMe_name이_51자이면_400과_VALIDATION_ERROR를_반환하고_errors에_name이_포함된다() throws Exception {
        String name51 = "가".repeat(51);

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name51 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void updateMe_email이_256자이면_400과_VALIDATION_ERROR를_반환하고_errors에_email이_포함된다() throws Exception {
        String email256 = "a".repeat(246) + "@test.com";

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email256 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void updateMe_phone이_21자이면_400과_VALIDATION_ERROR를_반환하고_errors에_phone이_포함된다() throws Exception {
        String phone21 = "0".repeat(21);

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone21 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
    }

    @Test
    void updateMe_이메일이_중복이면_409와_MEMBER_EMAIL_DUPLICATED를_반환한다() throws Exception {
        when(memberService.updateMyInfo(eq(MEMBER_ID), any()))
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

    private MeInfoResponse fullInfoResponse() {
        MemberGradeSummaryResponse grade = new MemberGradeSummaryResponse(
                1L, "씨앗", 1, 2L, "새싹", 10000L, 20000L, 50.0);
        return new MeInfoResponse(
                MEMBER_ID, "member0714a", "회원A", "member0714a@example.com", "010-0714-0001",
                LocalDate.of(1995, 7, 14), "MALE", "USER", "JOINED",
                grade, 3000, 2, 5, 3, 4, 1);
    }
}
