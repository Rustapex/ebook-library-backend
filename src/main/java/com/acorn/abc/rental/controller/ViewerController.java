package com.acorn.abc.rental.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.rental.dto.ProgressResponse;
import com.acorn.abc.rental.dto.ProgressUpdateRequest;
import com.acorn.abc.rental.dto.ViewerPageResponse;
import com.acorn.abc.rental.service.ViewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Viewer", description = "전자책 뷰어 API")
@RestController
@RequestMapping("/api/v1/rentals/{rentalId}")
@RequiredArgsConstructor
public class ViewerController {

    private final ViewerService viewerService;

    @Operation(summary = "뷰어 페이지 조회", description = "대여한 도서의 특정 페이지를 조회합니다.")
    @GetMapping("/pages/{pageNo}")
    public ResponseEntity<ApiResponse<ViewerPageResponse>> getPage(
            @PathVariable Long rentalId,
            @PathVariable int pageNo,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(viewerService.getPage(rentalId, pageNo, userDetails.getMemberId())));
    }

    @Operation(summary = "읽기 진행 저장", description = "현재 읽고 있는 페이지와 완독 여부를 저장합니다.")
    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<ProgressResponse>> saveProgress(
            @PathVariable Long rentalId,
            @Valid @RequestBody ProgressUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(viewerService.saveProgress(rentalId, request, userDetails.getMemberId())));
    }
}
