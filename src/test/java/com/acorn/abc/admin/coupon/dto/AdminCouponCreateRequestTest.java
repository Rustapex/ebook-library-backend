package com.acorn.abc.admin.coupon.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCouponCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void benefitUnit과_status는_허용값만_통과한다() {
        AdminCouponCreateRequest request =
                new AdminCouponCreateRequest("쿠폰", "WELCOME", 1000, "POINT", 14, "UNACTIVE");

        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void benefitUnit과_status가_허용값이면_통과한다() {
        AdminCouponCreateRequest request =
                new AdminCouponCreateRequest("쿠폰", "WELCOME", 1000, "AMOUNT", 14, "ACTIVE");

        assertThat(validator.validate(request)).isEmpty();
    }
}
