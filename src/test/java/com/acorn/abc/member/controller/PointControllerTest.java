package com.acorn.abc.member.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.PointSummaryResponse;
import com.acorn.abc.member.service.PointService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PointControllerTest {

    private final PointService pointService = mock(PointService.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PointController(pointService))
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

    // 컨트롤러 클래스에 @Validated가 있어야 Spring이 실행 시점에 이 제약을 실제로 검증한다(운영 코드에서 이미 적용).
    // 여기서는 @Min 제약이 page/size 파라미터에 정확히 붙어 있는지를 실행 파라미터 검증(Bean Validation)으로 직접 확인한다.
    @Test
    void getPointHistory_page가_음수이면_제약을_위반한다() throws NoSuchMethodException {
        Method method = PointController.class.getMethod(
                "getPointHistory", String.class, int.class, int.class, CustomUserDetails.class);
        Set<ConstraintViolation<PointController>> violations = validator.forExecutables()
                .validateParameters(new PointController(pointService), method,
                        new Object[]{null, -1, 10, null});

        assertThat(violations).isNotEmpty();
    }

    @Test
    void getPointHistory_size가_0이면_제약을_위반한다() throws NoSuchMethodException {
        Method method = PointController.class.getMethod(
                "getPointHistory", String.class, int.class, int.class, CustomUserDetails.class);
        Set<ConstraintViolation<PointController>> violations = validator.forExecutables()
                .validateParameters(new PointController(pointService), method,
                        new Object[]{null, 0, 0, null});

        assertThat(violations).isNotEmpty();
    }

    @Test
    void getPointHistory_정상_page_size는_200을_반환한다() throws Exception {
        when(pointService.getPointHistory(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(new PointSummaryResponse(0, PageResponse.of(List.of(), 0, 10, 0)));

        mockMvc.perform(get("/api/v1/me/points").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }
}
