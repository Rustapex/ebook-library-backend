package com.acorn.abc.admin.member.controller;

import com.acorn.abc.admin.member.dto.AdminMemberDetailResponse;
import com.acorn.abc.admin.member.dto.AdminMemberProfileResponse;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeResponse;
import com.acorn.abc.admin.member.dto.AdminMemberUsageSummaryResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.service.AdminMemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 회원 API 구현계획(2026-07-16) §4.1/§4.2 — HTTP·validation 계약 검증(001~003).
 */
class AdminMemberControllerTest {

    private static final long ADMIN_MEMBER_ID = 99L;

    private final AdminMemberService adminMemberService = mock(AdminMemberService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        AdminMemberController controller = (AdminMemberController) postProcessor.postProcessAfterInitialization(
                new AdminMemberController(adminMemberService), "adminMemberController");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        CustomUserDetails principal = new CustomUserDetails(ADMIN_MEMBER_ID, "ADMIN");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMembers_정상_요청은_200을_반환한다() throws Exception {
        when(adminMemberService.getAdminMembers(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMembers_size가_101이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMembers_page가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMemberDetail_tab_미지정이면_DEFAULT로_동작한다() throws Exception {
        AdminMemberDetailResponse detail = new AdminMemberDetailResponse(1L,
                new AdminMemberProfileResponse(1L, "user1", "홍길동", "user1@test.com", "01011112222",
                        LocalDate.of(2000, 1, 1), "M", "USER", "JOINED", "BASIC", LocalDateTime.now()),
                new AdminMemberUsageSummaryResponse(1000, 0, 0, 0, List.of()));
        when(adminMemberService.getAdminMemberDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.email").value("user1@test.com"));
    }

    @Test
    void getMemberDetail_잘못된_tab이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members/1").param("tab", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMemberDetail_존재하지_않으면_404를_반환한다() throws Exception {
        when(adminMemberService.getAdminMemberDetail(999L)).thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/members/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void changeStatus_요청_본문이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/members/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(adminMemberService, never()).changeMemberStatus(any(), any(), any());
    }

    @Test
    void changeStatus_sanctionType이_허용값이_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/members/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SANCTIONED\",\"reason\":\"사유\",\"sanctionType\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changeStatus_정상_요청은_200을_반환하고_인증principal의_memberId를_전달한다() throws Exception {
        when(adminMemberService.changeMemberStatus(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminMemberStatusChangeResponse(1L, "DEACTIVATED", null));

        mockMvc.perform(patch("/api/v1/admin/members/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\",\"reason\":\"휴면 처리\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));

        verify(adminMemberService).changeMemberStatus(eq(1L), any(), eq(ADMIN_MEMBER_ID));
    }
}
