package com.acorn.abc.admin.book.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookIsbnBulkApproveRequestTest {

    private final Validator validator;

    BookIsbnBulkApproveRequestTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void limit과_maxCandidates가_없으면_기본값이_채워진다() {
        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(null, null, null, "PAID", 5000, "AVAILABLE");

        assertThat(request.limit()).isEqualTo(30);
        assertThat(request.maxCandidates()).isEqualTo(100);
    }

    @Test
    void limit이_30을_초과하면_검증에_실패한다() {
        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(null, 31, 100, "PAID", 5000, "AVAILABLE");

        Set<ConstraintViolation<BookIsbnBulkApproveRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("limit"));
    }

    @Test
    void tempIds가_100개를_초과하면_검증에_실패한다() {
        List<Long> tooMany = java.util.stream.LongStream.range(0, 101).boxed().toList();
        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(tooMany, 30, 100, "PAID", 5000, "AVAILABLE");

        Set<ConstraintViolation<BookIsbnBulkApproveRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tempIds"));
    }
}
