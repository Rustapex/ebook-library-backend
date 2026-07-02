package com.acorn.abc.rental.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.rental.dto.RecentBookResponse;
import com.acorn.abc.rental.service.ReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reading", description = "최근 읽은 책 API")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @Operation(summary = "최근 읽은 책 조회", description = "최근 읽은 순으로 도서를 조회합니다.")
    @GetMapping("/recent-books")
    public ResponseEntity<ApiResponse<List<RecentBookResponse>>> getRecentBooks(
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(readingService.getRecentBooks(userDetails.getMemberId(), limit)));
    }
}
