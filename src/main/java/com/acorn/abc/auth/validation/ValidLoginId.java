package com.acorn.abc.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 회원가입(SignupRequest)과 로그인 아이디 중복확인(CheckLoginIdRequest)이 같은 아이디 검증
 * 계약을 공유하도록 조합한 제약이다(PR49 코드리뷰 후속수정계획 §4). 별도 ConstraintValidator
 * 없이 @NotBlank/@Size/@Pattern 조합만으로 검증하며, @ReportAsSingleViolation을 쓰지 않아
 * 기존처럼 필드별 개별 위반 메시지가 그대로 유지된다.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "아이디는 필수입니다.")
@Size(min = 5, max = 20, message = "아이디는 5~20자로 입력해주세요.")
@Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
public @interface ValidLoginId {

    String message() default "아이디 형식이 올바르지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
