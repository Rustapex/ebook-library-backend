package com.acorn.abc.request.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.service.BookRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookRequestControllerTest {

    private final BookRequestService bookRequestService = mock(BookRequestService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookRequestController(bookRequestService))
                .setControllerAdvice(new GlobalExceptionHandler())
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
    void createBookRequest_정상_요청은_201과_생성_응답을_반환한다() throws Exception {
        when(bookRequestService.createBookRequest(eq(1L), any()))
                .thenReturn(new BookRequestCreateResponse(10L, 20L, "REQUESTED", 1));

        mockMvc.perform(post("/api/v1/book-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 도서\",\"author\":\"저자\",\"publisher\":\"출판사\",\"reason\":\"신청 사유\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requestId").value(10))
                .andExpect(jsonPath("$.data.requestCandidateId").value(20))
                .andExpect(jsonPath("$.data.requestStatus").value("REQUESTED"));

        verify(bookRequestService).createBookRequest(eq(1L), any());
    }

    @Test
    void createBookRequest_제목이_공백이면_400을_반환하고_service를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/book-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"저자\",\"publisher\":\"출판사\",\"reason\":\"신청 사유\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(bookRequestService, never()).createBookRequest(any(), any());
    }

    @Test
    void createBookRequest_중복_신청이면_409_에러코드를_반환한다() throws Exception {
        when(bookRequestService.createBookRequest(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.BOOK_REQUEST_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/book-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 도서\",\"author\":\"저자\",\"publisher\":\"출판사\",\"reason\":\"신청 사유\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_REQUEST_ALREADY_EXISTS"));
    }
}
