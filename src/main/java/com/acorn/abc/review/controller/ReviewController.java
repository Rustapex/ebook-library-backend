package com.acorn.abc.review.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.review.dto.ReviewDeleteResponse;
import com.acorn.abc.review.dto.ReviewListResponse;
import com.acorn.abc.review.dto.ReviewRequest;
import com.acorn.abc.review.dto.ReviewResponse;
import com.acorn.abc.review.dto.ReviewUpdateRequest;
import com.acorn.abc.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "리뷰/별점 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "도서 리뷰 목록 조회", description = "별점 요약과 활성 리뷰 목록을 반환합니다.")
    @GetMapping("/api/v1/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getBookReviews(bookId, page, size, sort)));
    }

    @Operation(summary = "리뷰/별점 작성", description = "리뷰와 별점을 함께 생성합니다. 삭제된 리뷰가 있으면 같은 row를 복구합니다.")
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReviewResponse response = reviewService.createOrRestoreReview(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "리뷰 수정", description = "본인 리뷰 내용을 수정합니다. DELETED 리뷰는 ACTIVE로 복구됩니다.")
    @PatchMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReviewResponse response = reviewService.updateReview(userDetails.getMemberId(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "리뷰 삭제", description = "본인 리뷰를 삭제(status=DELETED) 처리합니다.")
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDeleteResponse>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReviewDeleteResponse response = reviewService.deleteReview(userDetails.getMemberId(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
