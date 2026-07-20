package com.acorn.abc.request.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.request.dto.BookRequestCreateRequest;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.service.BookRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BookRequest", description = "희망도서 API")
@RestController
@RequestMapping("/api/v1/book-requests")
@RequiredArgsConstructor
public class BookRequestController {

    private final BookRequestService bookRequestService;

    @Operation(summary = "희망도서 신청", description = "제목/저자/출판사 정규화 조합으로 후보를 집계합니다. 동일 조합 재신청은 409입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookRequestCreateResponse>> createBookRequest(
            @Valid @RequestBody BookRequestCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BookRequestCreateResponse response = bookRequestService.createBookRequest(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }
}
