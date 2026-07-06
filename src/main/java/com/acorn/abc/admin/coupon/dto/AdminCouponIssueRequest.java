package com.acorn.abc.admin.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminCouponIssueRequest(
        @NotEmpty(message = "발급 대상 회원을 선택해주세요.")
        List<Long> memberIds,

        @Min(value = 1, message = "발급 수량은 1개 이상이어야 합니다.")
        int quantity
) {
}
