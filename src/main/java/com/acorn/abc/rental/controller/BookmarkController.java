package com.acorn.abc.rental.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.rental.dto.BookmarkCreateRequest;
import com.acorn.abc.rental.dto.BookmarkDeleteResponse;
import com.acorn.abc.rental.dto.BookmarkResponse;
import com.acorn.abc.rental.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Bookmarks", description = "북마크 API")
@RestController
@RequestMapping("/api/v1/rentals/{rentalId}/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "북마크 추가", description = "현재 페이지를 북마크로 저장합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> addBookmark(
            @PathVariable Long rentalId,
            @Valid @RequestBody BookmarkCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BookmarkResponse response = bookmarkService.addBookmark(rentalId, request, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "북마크 삭제", description = "본인 대여 건의 북마크를 삭제합니다.")
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<BookmarkDeleteResponse>> deleteBookmark(
            @PathVariable Long rentalId,
            @PathVariable Long bookmarkId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BookmarkDeleteResponse response = bookmarkService.deleteBookmark(rentalId, bookmarkId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "북마크 목록 조회", description = "대여 건의 북마크 목록을 페이지 번호 오름차순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarks(
            @PathVariable Long rentalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(bookmarkService.getBookmarks(rentalId, userDetails.getMemberId())));
    }
}
