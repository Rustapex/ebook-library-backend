package com.acorn.abc.admin.book.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class BookIsbnTempBulkDeleteRequestTest {

    private final Validator validator;

    BookIsbnTempBulkDeleteRequestTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void tempIds가_비어_있으면_검증에_실패한다() {
        Set<ConstraintViolation<BookIsbnTempBulkDeleteRequest>> violations =
                validator.validate(new BookIsbnTempBulkDeleteRequest(List.of()));

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tempIds"));
    }

    @Test
    void tempIds가_100개를_초과하면_검증에_실패한다() {
        List<Long> tooMany = LongStream.rangeClosed(1, 101).boxed().toList();

        Set<ConstraintViolation<BookIsbnTempBulkDeleteRequest>> violations =
                validator.validate(new BookIsbnTempBulkDeleteRequest(tooMany));

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tempIds"));
    }

    @Test
    void null_또는_양수가_아닌_ID가_있으면_검증에_실패한다() {
        BookIsbnTempBulkDeleteRequest request =
                new BookIsbnTempBulkDeleteRequest(Arrays.asList(null, 0L, -1L));

        Set<ConstraintViolation<BookIsbnTempBulkDeleteRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(3);
        assertThat(violations).allMatch(v -> v.getPropertyPath().toString().startsWith("tempIds"));
    }

    @Test
    void tempIds가_1개에서_100개_사이의_양수이면_검증에_성공한다() {
        BookIsbnTempBulkDeleteRequest one = new BookIsbnTempBulkDeleteRequest(List.of(1L));
        BookIsbnTempBulkDeleteRequest hundred = new BookIsbnTempBulkDeleteRequest(
                LongStream.rangeClosed(1, 100).boxed().toList());

        assertThat(validator.validate(one)).isEmpty();
        assertThat(validator.validate(hundred)).isEmpty();
    }
}
