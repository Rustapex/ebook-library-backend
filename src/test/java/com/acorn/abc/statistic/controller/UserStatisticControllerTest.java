package com.acorn.abc.statistic.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.statistic.dto.UserReadingStatisticResponse;
import com.acorn.abc.statistic.service.UserStatisticService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 인증 누락(401) 자체는 Spring Security 필터 체인(JwtAuthenticationFilter/CustomAuthenticationEntryPoint) 책임이라
// standalone MockMvc 단위 테스트로는 재현하지 않는다(AttendanceControllerTest와 동일한 기존 관례).
// 인증 누락 케이스는 회원용독서통계 구현계획 §6 Swagger/Postman 수동 검증으로 확인한다.
class UserStatisticControllerTest {

    private final UserStatisticService userStatisticService = mock(UserStatisticService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        UserStatisticController controller = (UserStatisticController) postProcessor.postProcessAfterInitialization(
                new UserStatisticController(userStatisticService), "userStatisticController");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void getMyStatistics_periodType이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/statistics"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyStatistics_잘못된_periodType이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/statistics").param("periodType", "DAILY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyStatistics_정상_요청은_200을_반환하고_본인_memberId로_조회한다() throws Exception {
        when(userStatisticService.getStatistics(anyLong(), anyString(), any()))
                .thenReturn(new UserReadingStatisticResponse(
                        "TOTAL", LocalDate.of(2000, 1, 1), LocalDate.of(2026, 7, 15),
                        3L, 2L, 150L, 1L, 4L, 4L, 0.08,
                        List.of(), java.time.LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/me/statistics").param("periodType", "TOTAL"))
                .andExpect(status().isOk());

        // PR #45 리뷰 반영: 인증 principal(memberId=1L)이 그대로 서비스에 전달되는지 실제로 검증한다
        // (이전에는 anyLong()으로만 스텁해서 어떤 memberId가 넘어갔는지 확인하지 않았다).
        verify(userStatisticService).getStatistics(eq(1L), eq("TOTAL"), isNull());
    }
}
