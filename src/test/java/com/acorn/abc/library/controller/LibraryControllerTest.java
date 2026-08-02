package com.acorn.abc.library.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import com.acorn.abc.library.dto.LibrarySearchResponse;
import com.acorn.abc.library.service.ClientIpResolver;
import com.acorn.abc.library.service.LibraryRateLimiter;
import com.acorn.abc.library.service.PhysicalLibrarySearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryControllerTest {

    private final PhysicalLibrarySearchService physicalLibrarySearchService = mock(PhysicalLibrarySearchService.class);
    private final LibraryRateLimiter libraryRateLimiter = mock(LibraryRateLimiter.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.afterPropertiesSet();
        LibraryController controller = (LibraryController) postProcessor.postProcessAfterInitialization(
                new LibraryController(physicalLibrarySearchService, libraryRateLimiter, clientIpResolver),
                "libraryController");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(clientIpResolver.resolve(any(HttpServletRequest.class))).willReturn("203.0.113.1");
        given(libraryRateLimiter.tryConsume(anyString())).willReturn(true);
    }

    private LibrarySearchResponse sampleResponse() {
        return new LibrarySearchResponse(1L, "제목", "http://cover", "11", "INFONARU", false,
                0, 20, 0, 0, List.of(), List.of());
    }

    @Test
    void 정상_요청은_200을_반환한다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void region이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void region_형식이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 좌표가_한쪽만_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries")
                        .param("region", "11")
                        .param("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 도서가_없으면_404를_반환한다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void isbn이_없으면_409를_반환한다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willThrow(new BusinessException(ErrorCode.BOOK_ISBN_UNAVAILABLE));

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_ISBN_UNAVAILABLE"));
    }

    @Test
    void 외부_장애이고_fallback도_없으면_503을_반환한다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willThrow(new BusinessException(ErrorCode.LIBRARY_EXTERNAL_API_UNAVAILABLE));

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("LIBRARY_EXTERNAL_API_UNAVAILABLE"));
    }

    @Test
    void rate_limit_초과시_429를_반환한다() throws Exception {
        given(libraryRateLimiter.tryConsume(anyString())).willReturn(false);

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LIBRARY_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void dtlRegion_형식이_5자리가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11").param("dtlRegion", "111"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void size가_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void size가_101이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void size가_100이면_통과한다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 100, null, null))
                .willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11").param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void latitude가_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries")
                        .param("region", "11")
                        .param("latitude", "91")
                        .param("longitude", "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void longitude가_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/1/libraries")
                        .param("region", "11")
                        .param("latitude", "37.5")
                        .param("longitude", "181"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 공개_응답에는_별도_printIsbn13_필드가_없다() throws Exception {
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.printIsbn13").doesNotExist());
    }
}
