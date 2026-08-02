package com.acorn.abc.request.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.request.dto.BookRequestHistoryItemResponse;
import com.acorn.abc.request.service.BookRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyBookRequest", description = "내 희망도서 신청 내역 API")
@RestController
@RequestMapping("/api/v1/me/book-requests")
@RequiredArgsConstructor
public class MyBookRequestController {

    private final BookRequestService bookRequestService;

    @Operation(summary = "내 희망도서 신청 내역 조회", description = "status는 후보 상태를 조인해 계산한 값(IN_REVIEW 포함)으로 필터링합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookRequestHistoryItemResponse>>> getMyBookRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PageResponse<BookRequestHistoryItemResponse> response =
                bookRequestService.getMyBookRequests(userDetails.getMemberId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
