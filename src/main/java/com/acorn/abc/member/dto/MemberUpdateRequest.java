package com.acorn.abc.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record MemberUpdateRequest(

        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        String phone,

        @Pattern(regexp = "^(MALE|FEMALE|NONE)$", message = "성별은 MALE, FEMALE, NONE 중 하나여야 합니다.")
        String gender
) {}
