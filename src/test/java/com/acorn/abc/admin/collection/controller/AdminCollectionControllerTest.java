package com.acorn.abc.admin.collection.controller;

import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionDetailResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSummaryResponse;
import com.acorn.abc.collection.service.AdminCollectionService;
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
 * 관리자 컬렉션 API 구현계획(2026-07-16) §9.1 — HTTP·validation·auth principal 계약 검증.
 * 인증/인가(401/403)는 실제 SecurityConfig가 필요해 AdminCollectionSecurityIntegrationTest에서 다룬다.
 */
class AdminCollectionControllerTest {

    private static final long ADMIN_MEMBER_ID = 99L;

    private final AdminCollectionService adminCollectionService = mock(AdminCollectionService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        AdminCollectionController controller = (AdminCollectionController) postProcessor.postProcessAfterInitialization(
                new AdminCollectionController(adminCollectionService), "adminCollectionController");

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
    void getCollections_정상_요청은_200을_반환한다() throws Exception {
        when(adminCollectionService.getAdminCollections(any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/v1/admin/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getCollections_size가_101이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/collections").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getCollections_page가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/collections").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCollectionDetail_없는_컬렉션이면_404를_반환한다() throws Exception {
        when(adminCollectionService.getAdminCollectionDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/collections/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void getCollectionDetail_정상_요청은_200을_반환한다() throws Exception {
        when(adminCollectionService.getAdminCollectionDetail(1L)).thenReturn(
                new AdminCollectionDetailResponse(1L, "이름", "SERIES", null, null, null, null, "ACTIVE",
                        1L, LocalDateTime.now(), null, List.of()));

        mockMvc.perform(get("/api/v1/admin/collections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectionId").value(1));
    }

    @Test
    void createCollection_정상_요청은_201을_반환하고_adminMemberId를_전달한다() throws Exception {
        when(adminCollectionService.createAdminCollection(any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminCollectionSaveResponse(1L, "ACTIVE", LocalDateTime.now(), null));

        mockMvc.perform(post("/api/v1/admin/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"신규 시리즈\",\"collectionType\":\"SERIES\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminCollectionService).createAdminCollection(any(), eq(ADMIN_MEMBER_ID));
    }

    @Test
    void createCollection_name이_없으면_400과_errors에_name이_포함된다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectionType\":\"SERIES\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        verify(adminCollectionService, never()).createAdminCollection(any(), anyLong());
    }

    @Test
    void createCollection_collectionType이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"이름\",\"collectionType\":\"NOVEL\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createCollection_할인율이_100을_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"이름\",\"collectionType\":\"EVENT\",\"discountRate\":101,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateCollection_정상_요청은_200을_반환한다() throws Exception {
        when(adminCollectionService.updateAdminCollection(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminCollectionSaveResponse(1L, "ACTIVE", LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(put("/api/v1/admin/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수정\",\"collectionType\":\"SERIES\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCollectionBooks_요청_목록이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/admin/collections/1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"books\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminCollectionService, never()).upsertCollectionBooks(anyLong(), any(), anyLong());
    }

    @Test
    void updateCollectionBooks_정상_요청은_200을_반환한다() throws Exception {
        when(adminCollectionService.upsertCollectionBooks(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminCollectionBooksUpdateResponse(1L, 2L));

        mockMvc.perform(put("/api/v1/admin/collections/1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"books\":[{\"bookId\":10,\"displayOrder\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mappedCount").value(2));
    }

    @Test
    void patchCollection_완전히_빈_요청은_400을_반환한다() throws Exception {
        when(adminCollectionService.patchCollection(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR));

        mockMvc.perform(patch("/api/v1/admin/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void patchCollection_status만_있어도_200을_반환한다() throws Exception {
        when(adminCollectionService.patchCollection(eq(1L), any(), eq(ADMIN_MEMBER_ID)))
                .thenReturn(new AdminCollectionPatchResponse(1L, "HIDDEN", 0L));

        mockMvc.perform(patch("/api/v1/admin/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }
}
