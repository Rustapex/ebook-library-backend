package com.acorn.abc.admin.book.controller;

import com.acorn.abc.admin.book.dto.EpubUploadResponse;
import com.acorn.abc.admin.book.service.AdminBookEpubService;
import com.acorn.abc.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Book EPUB", description = "관리자 EPUB 파일 API")
@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
public class AdminBookEpubController {

    private final AdminBookEpubService adminBookEpubService;

    @Operation(summary = "EPUB 등록 또는 교체", description = "Swagger/Postman에서 EPUB 파일을 multipart로 업로드합니다.")
    @PutMapping(value = "/{bookId}/epub", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EpubUploadResponse>> upload(
            @PathVariable Long bookId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminBookEpubService.upload(bookId, file)));
    }
}
