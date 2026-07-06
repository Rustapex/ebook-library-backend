package com.acorn.abc.admin.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminCouponCreateRequest(
        @NotBlank(message = "쿠폰명은 필수입니다.")
        String couponName,

        @NotBlank(message = "쿠폰 유형은 필수입니다.")
        String couponType,

        @Min(value = 0, message = "혜택값은 0 이상이어야 합니다.")
        int benefitValue,

        @NotBlank(message = "혜택 단위는 필수입니다.")
        String benefitUnit,

        @Min(value = 1, message = "유효기간은 1일 이상이어야 합니다.")
        int validDays,

        @NotBlank(message = "상태는 필수입니다.")
        String status
) {
}
