package com.acorn.abc.coupon.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.coupon.dto.MyCouponResponse;
import com.acorn.abc.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupon", description = "쿠폰 API")
@RestController
@RequestMapping("/api/v1/me/coupons")
@RequiredArgsConstructor
@Validated
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "내 쿠폰 목록 조회", description = "상태/사용가능여부로 필터링한 회원 쿠폰 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MyCouponResponse>>> getMyCoupons(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long usableForBookId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageResponse<MyCouponResponse> response =
                couponService.getMyCoupons(userDetails.getMemberId(), status, usableForBookId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
