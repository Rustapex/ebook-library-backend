package com.acorn.abc.member.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberGradeSummaryResponse;
import com.acorn.abc.member.dto.MemberWithdrawResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                        .content("{\"name\":\"회원A수정\",\"email\":\"updated@example.com\",\"phone\":\"010-0714-9999\"}"))
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

    // 회원정보수정 보완계획 §6.1: 본문 누락은 Swagger UI가 아니라 Postman 등 실제 HTTP
    // 클라이언트로만 재현 가능하므로, 여기서는 .content() 자체를 생략해 같은 상황을 만든다.
    @Test
    void updateMe_요청_본문이_없으면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMe_빈_JSON이면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMe_name이_공백만_있으면_400과_errors에_name이_포함된다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void updateMe_email이_공백만_있으면_400과_errors에_email이_포함된다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void updateMe_phone이_빈문자열이면_200을_반환한다() throws Exception {
        when(memberService.updateMyInfo(eq(MEMBER_ID), any())).thenReturn(fullInfoResponse());

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateMe_gender가_빈문자열이면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMe_gender가_NONE이면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"NONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // 프로필 성별은 더 이상 수정 가능한 필드가 아니다 — 유효해 보이는 값(MALE/FEMALE)도 거부해야 한다
    // (2026-07-21 회원가입·프로필·비밀번호 버그 대응 구현계획 §2.2).
    @Test
    void updateMe_gender가_유효한_값이어도_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"MALE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(memberService, never()).updateMyInfo(eq(MEMBER_ID), any());
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

    @Test
    void withdraw_정상_요청이면_200과_WITHDRAWN_상태를_반환한다() throws Exception {
        when(memberService.withdraw(eq(MEMBER_ID), any()))
                .thenReturn(new MemberWithdrawResponse("WITHDRAWN", java.time.LocalDateTime.of(2026, 7, 22, 10, 0)));

        mockMvc.perform(delete("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Passw0rd!!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    void withdraw_currentPassword가_없으면_400과_VALIDATION_ERROR를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(memberService, never()).withdraw(eq(MEMBER_ID), any());
    }

    @Test
    void withdraw_비밀번호가_틀리면_400과_AUTH_INVALID_PASSWORD를_반환한다() throws Exception {
        when(memberService.withdraw(eq(MEMBER_ID), any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_PASSWORD));

        mockMvc.perform(delete("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_PASSWORD"));
    }

    @Test
    void withdraw_진행중인_대여가_있으면_409를_반환한다() throws Exception {
        when(memberService.withdraw(eq(MEMBER_ID), any()))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS));

        mockMvc.perform(delete("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Passw0rd!!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS"));
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
