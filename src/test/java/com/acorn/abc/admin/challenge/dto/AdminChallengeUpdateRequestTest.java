package com.acorn.abc.admin.challenge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminChallengeUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rewards_배열에_null_원소가_있으면_거부한다() {
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "ACTIVE",
                Arrays.asList(new AdminChallengeRewardItemRequest("POINT", 100, null, 1), null));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rewards_배열이_전부_정상이면_통과한다() {
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "ACTIVE",
                List.of(new AdminChallengeRewardItemRequest("POINT", 100, null, 1)));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void challengeType이_DAILY_TOTAL이_아니면_거부한다() {
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "WEEKLY", "ATTENDANCE", 1, "ACTIVE",
                List.of(new AdminChallengeRewardItemRequest("POINT", 100, null, 1)));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void status가_ACTIVE_INACTIVE가_아니면_거부한다() {
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "출석 챌린지", "DAILY", "ATTENDANCE", 1, "DELETED",
                List.of(new AdminChallengeRewardItemRequest("POINT", 100, null, 1)));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void challengeType_TOTAL과_status_INACTIVE는_통과한다() {
        AdminChallengeUpdateRequest request = new AdminChallengeUpdateRequest(
                "누적 챌린지", "TOTAL", "ATTENDANCE", 10, "INACTIVE",
                List.of(new AdminChallengeRewardItemRequest("POINT", 100, null, 1)));

        assertThat(validator.validate(request)).isEmpty();
    }
}
