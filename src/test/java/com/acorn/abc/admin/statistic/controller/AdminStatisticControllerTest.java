package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.service.AdminStatisticService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminStatisticControllerTest {

    private final AdminStatisticService adminStatisticService = mock(AdminStatisticService.class);

    private MockMvc validatedMockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        AdminStatisticController controller = (AdminStatisticController) postProcessor.postProcessAfterInitialization(
                new AdminStatisticController(adminStatisticService), "adminStatisticController");

        validatedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStatistics_periodType이_없으면_400을_반환한다() throws Exception {
        validatedMockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatistics_periodType이_허용값이_아니면_400을_반환한다() throws Exception {
        validatedMockMvc.perform(get("/api/v1/admin/statistics").param("periodType", "DAILY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatistics_ageBand이_허용값이_아니면_400을_반환한다() throws Exception {
        validatedMockMvc.perform(get("/api/v1/admin/statistics")
                        .param("periodType", "TOTAL")
                        .param("ageBand", "TEENAGER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatistics_정상_요청은_200을_반환한다() throws Exception {
        when(adminStatisticService.getStatistics(any(), any(), any())).thenReturn(sampleResponse("TOTAL", "ALL"));

        validatedMockMvc.perform(get("/api/v1/admin/statistics").param("periodType", "TOTAL"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatistics_WEEKLY와_baseDate로_호출하면_200을_반환한다() throws Exception {
        when(adminStatisticService.getStatistics(any(), any(), any())).thenReturn(sampleResponse("WEEKLY", "ALL"));

        validatedMockMvc.perform(get("/api/v1/admin/statistics")
                        .param("periodType", "WEEKLY")
                        .param("baseDate", "2026-07-12"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatistics_MONTHLY와_baseDate_ageBand로_호출하면_200을_반환한다() throws Exception {
        when(adminStatisticService.getStatistics(any(), any(), any())).thenReturn(sampleResponse("MONTHLY", "20S"));

        validatedMockMvc.perform(get("/api/v1/admin/statistics")
                        .param("periodType", "MONTHLY")
                        .param("baseDate", "2026-07-12")
                        .param("ageBand", "20S"))
                .andExpect(status().isOk());
    }

    private AdminStatisticResponse sampleResponse(String periodType, String ageBand) {
        return new AdminStatisticResponse(
                periodType, ageBand, LocalDate.of(2000, 1, 1), LocalDate.now(),
                0, 0, 0, 0, 0, 0, 0, 0L, 0, 0, 0L, 0.0,
                List.of(), List.of(), List.of(), LocalDateTime.now());
    }
}
