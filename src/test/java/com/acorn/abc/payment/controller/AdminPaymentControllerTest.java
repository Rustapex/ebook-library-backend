package com.acorn.abc.payment.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.payment.service.AdminPaymentService;
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

class AdminPaymentControllerTest {

    private final AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPaymentController(adminPaymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPayments_정상_page_size는_200을_반환한다() throws Exception {
        when(adminPaymentService.getAdminPayments(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/v1/admin/payments").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getPayments_page가_음수이면_실제_400을_반환한다() throws Exception {
        MockMvc validatedMockMvc = MockMvcBuilders.standaloneSetup(methodValidatedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        validatedMockMvc.perform(get("/api/v1/admin/payments").param("page", "-1").param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    private AdminPaymentController methodValidatedController() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        return (AdminPaymentController) postProcessor.postProcessAfterInitialization(
                new AdminPaymentController(adminPaymentService), "adminPaymentController");
    }
}
