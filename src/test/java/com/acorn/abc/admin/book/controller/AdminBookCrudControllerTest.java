package com.acorn.abc.admin.book.controller;

import com.acorn.abc.admin.book.dto.AdminBookSaveResponse;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeResponse;
import com.acorn.abc.book.service.AdminBookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 도서 CRUD API(API-ADMIN-BOOK-001~004, {@link AdminBookController}) HTTP·validation·auth
 * principal 계약 검증(관리자 도서 API 구현계획 §5.1). 인증/인가(401/403)는 실제 SecurityConfig가 필요해
 * AdminBookCrudSecurityIntegrationTest에서 다룬다. 같은 admin.book.controller 패키지의 기존
 * AdminBookControllerTest(외부 ISBN 수집 기능)와는 별개 클래스다.
 */
class AdminBookCrudControllerTest {

    private static final long ADMIN_MEMBER_ID = 99L;
    private static final String VALID_BODY =
            "{\"title\":\"제목\",\"publisherName\":\"출판사\",\"authors\":[\"작가\"],\"categoryIds\":[],"
                    + "\"rentalType\":\"FREE\",\"rentalPrice\":0,\"defaultRentalDays\":7}";

    private final AdminBookService adminBookService = mock(AdminBookService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        AdminBookController controller = (AdminBookController) postProcessor.postProcessAfterInitialization(
                new AdminBookController(adminBookService), "adminBookController");

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
    void getBooks_정상_요청은_200을_반환한다() throws Exception {
        when(adminBookService.getAdminBooks(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getBooks_size가_101이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/books").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getBooks_page가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/books").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBook_정상_요청은_201을_반환하고_adminMemberId를_전달한다() throws Exception {
        when(adminBookService.createAdminBook(any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminBookSaveResponse(1L, "INACTIVE", LocalDateTime.now(), null));

        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(adminBookService).createAdminBook(any(), eq(ADMIN_MEMBER_ID));
    }

    @Test
    void createBook_title이_없으면_400과_errors에_title이_포함된다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publisherName\":\"출판사\",\"authors\":[\"작가\"],\"categoryIds\":[],"
                                + "\"rentalType\":\"FREE\",\"rentalPrice\":0,\"defaultRentalDays\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(adminBookService, never()).createAdminBook(any(), anyLong());
    }

    @Test
    void createBook_rentalType이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"publisherName\":\"출판사\",\"authors\":[\"작가\"],\"categoryIds\":[],"
                                + "\"rentalType\":\"RENT\",\"rentalPrice\":0,\"defaultRentalDays\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createBook_저자가_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"publisherName\":\"출판사\",\"authors\":[],\"categoryIds\":[],"
                                + "\"rentalType\":\"FREE\",\"rentalPrice\":0,\"defaultRentalDays\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createBook_ISBN이_13자리_숫자가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"isbn\":\"abc\",\"publisherName\":\"출판사\",\"authors\":[\"작가\"],"
                                + "\"categoryIds\":[],\"rentalType\":\"FREE\",\"rentalPrice\":0,\"defaultRentalDays\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createBook_ISBN_중복이면_409를_반환한다() throws Exception {
        when(adminBookService.createAdminBook(any(), eq(ADMIN_MEMBER_ID)))
                .thenThrow(new BusinessException(ErrorCode.BOOK_ISBN_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_ISBN_ALREADY_EXISTS"));
    }

    @Test
    void updateBook_존재하지_않는_도서면_404를_반환한다() throws Exception {
        when(adminBookService.updateAdminBook(eq(999L), any(), eq(ADMIN_MEMBER_ID)))
                .thenThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        mockMvc.perform(put("/api/v1/admin/books/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void updateBook_정상_요청은_200을_반환한다() throws Exception {
        when(adminBookService.updateAdminBook(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminBookSaveResponse(1L, "AVAILABLE", LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(put("/api/v1/admin/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void updateBook_동시수정_충돌이면_409를_반환한다() throws Exception {
        when(adminBookService.updateAdminBook(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenThrow(new BusinessException(ErrorCode.CONCURRENT_UPDATE));

        mockMvc.perform(put("/api/v1/admin/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void changeBookStatus_상태값이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/books/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminBookService, never()).changeAdminBookStatus(anyLong(), any(), anyLong());
    }

    @Test
    void changeBookStatus_정상_요청은_200을_반환한다() throws Exception {
        when(adminBookService.changeAdminBookStatus(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminBookStatusChangeResponse(1L, "HIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/books/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"운영 점검\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }
}
