package com.acorn.abc.admin.coupon.controller;

import com.acorn.abc.admin.coupon.dto.AdminCouponCreateRequest;
import com.acorn.abc.admin.coupon.dto.AdminCouponCreateResponse;
import com.acorn.abc.admin.coupon.dto.AdminCouponIssueRequest;
import com.acorn.abc.admin.coupon.dto.AdminCouponIssueResponse;
import com.acorn.abc.admin.coupon.dto.AdminCouponSummaryResponse;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminCoupon", description = "관리자 쿠폰 API")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@Validated
public class AdminCouponController {

    private final CouponService couponService;

    @Operation(summary = "쿠폰 목록 조회", description = "쿠폰 정의와 발급/사용/만료 집계를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCouponSummaryResponse>>> getCoupons(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String couponType,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminCouponSummaryResponse> response = couponService.getAdminCoupons(status, couponType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "쿠폰 등록", description = "새 쿠폰 정의를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCouponCreateResponse>> createCoupon(
            @Valid @RequestBody AdminCouponCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        Long couponId = couponService.createCoupon(request, adminDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(new AdminCouponCreateResponse(couponId)));
    }

    @Operation(summary = "회원에게 쿠폰 발급", description = "대상 회원들에게 지정한 수량만큼 쿠폰을 발급합니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<AdminCouponIssueResponse>> issueCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody AdminCouponIssueRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        int issuedCount = couponService.issueCoupon(couponId, request.memberIds(), request.quantity(), adminDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(new AdminCouponIssueResponse(issuedCount)));
    }
}
