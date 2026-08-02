package com.acorn.abc.rental.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.rental.dto.EpubDownloadMetadata;
import com.acorn.abc.rental.dto.ProgressResponse;
import com.acorn.abc.rental.dto.ProgressUpdateRequest;
import com.acorn.abc.rental.dto.ViewerSessionResponse;
import com.acorn.abc.rental.service.ViewerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/rentals/{rentalId}")
@RequiredArgsConstructor
public class ViewerController {

    private static final MediaType EPUB_MEDIA_TYPE = MediaType.parseMediaType("application/epub+zip");
    private final ViewerService viewerService;

    @GetMapping("/viewer")
    public ResponseEntity<ApiResponse<ViewerSessionResponse>> getSession(
            @PathVariable Long rentalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(viewerService.getSession(rentalId, userDetails.getMemberId())));
    }

    @GetMapping("/epub")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long rentalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EpubDownloadMetadata metadata = viewerService.prepareDownload(rentalId, userDetails.getMemberId());
        StreamingResponseBody body = output -> viewerService.writeEpub(metadata, output);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(metadata.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(EPUB_MEDIA_TYPE)
                .contentLength(metadata.fileSize())
                .eTag("\"" + metadata.sha256() + "\"")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<ProgressResponse>> saveProgress(
            @PathVariable Long rentalId,
            @Valid @RequestBody ProgressUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(viewerService.saveProgress(
                rentalId, request, userDetails.getMemberId())));
    }
}
