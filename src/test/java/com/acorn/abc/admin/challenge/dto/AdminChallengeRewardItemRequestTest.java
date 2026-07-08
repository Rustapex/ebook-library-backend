package com.acorn.abc.admin.challenge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminChallengeRewardItemRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rewardType_POINT만_허용한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("coupon", null, 1L, 1);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rewardType_POINT는_통과한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("POINT", 100, null, 1);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rewardPoint가_0이면_거부한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("POINT", 0, null, 1);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rewardPoint가_음수이면_거부한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("POINT", -100, null, 1);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
