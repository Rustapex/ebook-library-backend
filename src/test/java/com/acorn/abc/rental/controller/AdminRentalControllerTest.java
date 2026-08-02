package com.acorn.abc.rental.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.rental.service.AdminRentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// page/size 제약 위반 시 실제 400 응답 검증은 AdminCouponControllerTest와 동일하게 @Validated AOP 프록시를
// 직접 만들어 재현한다(standaloneSetup()은 컨트롤러 인스턴스를 그대로 써서 프록시가 걸리지 않는다).
class AdminRentalControllerTest {

    private final AdminRentalService adminRentalService = mock(AdminRentalService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRentalController(adminRentalService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRentals_정상_page_size는_200을_반환한다() throws Exception {
        when(adminRentalService.getAdminRentals(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/v1/admin/rentals").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRentals_page가_음수이면_실제_400을_반환한다() throws Exception {
        MockMvc validatedMockMvc = MockMvcBuilders.standaloneSetup(methodValidatedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        validatedMockMvc.perform(get("/api/v1/admin/rentals").param("page", "-1").param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRentals_size가_0이면_실제_400을_반환한다() throws Exception {
        MockMvc validatedMockMvc = MockMvcBuilders.standaloneSetup(methodValidatedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        validatedMockMvc.perform(get("/api/v1/admin/rentals").param("page", "0").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    private AdminRentalController methodValidatedController() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        return (AdminRentalController) postProcessor.postProcessAfterInitialization(
                new AdminRentalController(adminRentalService), "adminRentalController");
    }
}
