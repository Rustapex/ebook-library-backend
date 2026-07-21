package com.acorn.abc.member.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MemberUpdateRequestValidator implements ConstraintValidator<ValidMemberUpdateRequest, MemberUpdateRequest> {

    @Override
    public boolean isValid(MemberUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        return request.name() != null || request.email() != null || request.phone() != null;
    }
}
