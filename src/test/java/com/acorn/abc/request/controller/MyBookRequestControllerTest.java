package com.acorn.abc.request.controller;

import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.request.dto.BookRequestHistoryItemResponse;
import com.acorn.abc.request.service.BookRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyBookRequestControllerTest {

    private final BookRequestService bookRequestService = mock(BookRequestService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MyBookRequestController(bookRequestService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        CustomUserDetails principal = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyBookRequests_IN_REVIEW_필터와_PageResponse를_반환한다() throws Exception {
        BookRequestHistoryItemResponse item = new BookRequestHistoryItemResponse(
                1L, "도서", "저자", "출판사", "사유", LocalDateTime.now(), "IN_REVIEW", 2, null, null);
        when(bookRequestService.getMyBookRequests(1L, "IN_REVIEW", 0, 10))
                .thenReturn(PageResponse.of(List.of(item), 0, 10, 1));

        mockMvc.perform(get("/api/v1/me/book-requests")
                        .param("status", "IN_REVIEW").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(bookRequestService).getMyBookRequests(1L, "IN_REVIEW", 0, 10);
    }
}
