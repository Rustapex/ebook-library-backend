package com.acorn.abc.admin.book.controller;

import com.acorn.abc.admin.book.dto.EpubUploadResponse;
import com.acorn.abc.admin.book.service.AdminBookEpubService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBookEpubControllerTest {

    private final AdminBookEpubService adminBookEpubService = mock(AdminBookEpubService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBookEpubController(adminBookEpubService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_정상EPUB이면_응답계약을반환한다() throws Exception {
        MockMultipartFile file = epubFile();
        when(adminBookEpubService.upload(eq(17L), any(MultipartFile.class)))
                .thenReturn(new EpubUploadResponse(17L, "book.epub", 128L, "a".repeat(64), 3));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/books/17/epub").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.bookId").value(17))
                .andExpect(jsonPath("$.data.fileName").value("book.epub"))
                .andExpect(jsonPath("$.data.fileSize").value(128))
                .andExpect(jsonPath("$.data.sha256").value("a".repeat(64)))
                .andExpect(jsonPath("$.data.epubVersion").value(3));

        verify(adminBookEpubService).upload(eq(17L), any(MultipartFile.class));
    }

    @Test
    void upload_filePart가없으면_400공통응답을반환한다() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/books/17/epub"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void upload_유효하지않은EPUB이면_400오류코드를반환한다() throws Exception {
        when(adminBookEpubService.upload(eq(17L), any(MultipartFile.class)))
                .thenThrow(new BusinessException(ErrorCode.BOOK_EPUB_INVALID));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/books/17/epub").file(epubFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BOOK_EPUB_INVALID"));
    }

    @Test
    void upload_크기제한초과이면_413오류코드를반환한다() throws Exception {
        when(adminBookEpubService.upload(eq(17L), any(MultipartFile.class)))
                .thenThrow(new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/books/17/epub").file(epubFile()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BOOK_EPUB_TOO_LARGE"));
    }

    private MockMultipartFile epubFile() {
        return new MockMultipartFile(
                "file", "book.epub", MediaType.parseMediaType("application/epub+zip").toString(),
                "epub-content".getBytes());
    }
}
