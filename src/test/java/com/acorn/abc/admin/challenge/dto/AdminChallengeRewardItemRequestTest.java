package com.acorn.abc.admin.challenge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminChallengeRewardItemRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rewardType_POINT_또는_coupon_외에는_거부한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("GOODS", null, 1L, 1);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rewardType_POINT는_통과한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("POINT", 100, null, 1);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rewardType_coupon은_통과한다() {
        AdminChallengeRewardItemRequest request =
                new AdminChallengeRewardItemRequest("coupon", null, 1L, 1);

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
