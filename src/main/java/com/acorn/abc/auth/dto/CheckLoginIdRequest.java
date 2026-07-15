package com.acorn.abc.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입과 판정이 갈리지 않도록 SignupRequest.loginId와 동일한 검증 규칙을 유지한다.
public record CheckLoginIdRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 5, max = 20, message = "아이디는 5~20자로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
        String loginId
) {
}
