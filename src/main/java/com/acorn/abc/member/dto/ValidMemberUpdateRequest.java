package com.acorn.abc.member.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * name/email/phone/gender가 전부 null이면(빈 JSON {} 포함) 변경할 값이 없다고 보고
 * 거부한다(회원정보수정 보완계획 §5.2-3).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MemberUpdateRequestValidator.class)
public @interface ValidMemberUpdateRequest {

    String message() default "변경할 값이 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
