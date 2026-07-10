package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDashboardControllerTest {

    private final AdminDashboardService adminDashboardService = mock(AdminDashboardService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminDashboardService)).build();
    }

    @Test
    void getDashboard_정상_요청은_200을_반환한다() throws Exception {
        AdminStatisticResponse statistics = new AdminStatisticResponse(
                "TOTAL", "ALL", LocalDate.of(2000, 1, 1), LocalDate.now(),
                0, 0, 0, 0, 0, 0, 0, 0L, 0, 0, LocalDateTime.now());
        when(adminDashboardService.getDashboard())
                .thenReturn(new AdminDashboardResponse(statistics, List.of()));

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk());
    }
}
