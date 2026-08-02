package com.acorn.abc.admin.coupon.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCouponIssueRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void memberIds_배열에_null_원소가_있으면_거부한다() {
        AdminCouponIssueRequest request = new AdminCouponIssueRequest(Arrays.asList(1L, null), 1);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void memberIds_배열이_전부_정상이면_통과한다() {
        AdminCouponIssueRequest request = new AdminCouponIssueRequest(List.of(1L, 2L), 1);

        assertThat(validator.validate(request)).isEmpty();
    }
}
