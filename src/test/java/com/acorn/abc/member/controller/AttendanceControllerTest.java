package com.acorn.abc.member.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.AttendanceMonthResponse;
import com.acorn.abc.member.service.AttendanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GlobalExceptionHandler의 1차 핸들러(MethodArgumentTypeMismatchException/MissingServletRequestParameterException)
// 회귀 테스트. yearMonth가 담당 도메인(챌린지/출석/포인트/쿠폰) 안에서 유일하게 required(@RequestParam,
// defaultValue 없음)인 파라미터라 이 엔드포인트로 두 예외 모두 재현한다.
class AttendanceControllerTest {

    private final AttendanceService attendanceService = mock(AttendanceService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AttendanceController(attendanceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        CustomUserDetails principal = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMonthlyAttendance_yearMonth가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/attendance"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthlyAttendance_yearMonth_형식이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/attendance").param("yearMonth", "2026/07"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthlyAttendance_정상_yearMonth는_200을_반환한다() throws Exception {
        when(attendanceService.getMonthlyAttendance(anyLong(), any(YearMonth.class)))
                .thenReturn(new AttendanceMonthResponse(List.of(LocalDate.of(2026, 7, 1)), true, 1));

        mockMvc.perform(get("/api/v1/me/attendance").param("yearMonth", "2026-07"))
                .andExpect(status().isOk());
    }
}
