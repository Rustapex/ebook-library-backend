package com.acorn.abc.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(

        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
        String email,

        @Size(max = 20, message = "전화번호는 20자 이하로 입력해주세요.")
        String phone,

        @Pattern(regexp = "^(MALE|FEMALE|NONE)$", message = "성별은 MALE, FEMALE, NONE 중 하나여야 합니다.")
        String gender
) {}
