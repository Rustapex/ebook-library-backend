package com.acorn.abc.auth.validation;

import com.acorn.abc.auth.dto.CheckLoginIdRequest;
import com.acorn.abc.auth.dto.SignupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR49 코드리뷰 후속수정계획(2026-07-15) §8.2 — @ValidLoginId 조합 애노테이션 검증.
 * CheckLoginIdRequest는 loginId 하나만 가진 record라 다른 필드 위반 없이 순수하게 검증할 수 있다.
 */
class ValidLoginIdTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 빈_문자열과_공백은_실패한다(String loginId) {
        assertThat(validate(loginId)).isNotEmpty();
    }

    @Test
    void null은_실패한다() {
        assertThat(validate(null)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "abcdefghijklmnopqrstu"})
    void 길이가_4자_또는_21자이면_실패한다(String loginId) {
        assertThat(validate(loginId)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcde", "abcdefghij1234567890"})
    void 길이가_5자_또는_20자이면_성공한다(String loginId) {
        assertThat(validate(loginId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "ABCdef01", "test999"})
    void 영문_숫자_조합은_성공한다(String loginId) {
        assertThat(validate(loginId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마", "member_1", "member-1", "member 1"})
    void 한글_밑줄_특수문자는_실패한다(String loginId) {
        assertThat(validate(loginId)).isNotEmpty();
    }

    @Test
    void SignupRequest와_CheckLoginIdRequest는_loginId에_같은_애노테이션을_사용한다() {
        RecordComponent signupLoginId = findComponent(SignupRequest.class, "loginId");
        RecordComponent checkLoginId = findComponent(CheckLoginIdRequest.class, "loginId");

        assertThat(signupLoginId.isAnnotationPresent(ValidLoginId.class)).isTrue();
        assertThat(checkLoginId.isAnnotationPresent(ValidLoginId.class)).isTrue();
    }

    @Test
    void 기존_검증_메시지가_유지된다() {
        assertThat(messagesOf(validate(""))).contains("아이디는 필수입니다.");
        assertThat(messagesOf(validate("abcd"))).contains("아이디는 5~20자로 입력해주세요.");
        assertThat(messagesOf(validate("member_1"))).contains("아이디는 영문과 숫자만 사용할 수 있습니다.");
    }

    private Set<ConstraintViolation<CheckLoginIdRequest>> validate(String loginId) {
        return validator.validate(new CheckLoginIdRequest(loginId));
    }

    private List<String> messagesOf(Set<ConstraintViolation<CheckLoginIdRequest>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }

    private static RecordComponent findComponent(Class<?> recordClass, String name) {
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        throw new IllegalStateException("component not found: " + name);
    }
}
