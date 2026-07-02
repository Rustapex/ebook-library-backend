package com.acorn.abc.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        // TODO: rememberMe=true 시 만료 연장 정책 확정 후 구현
        Boolean rememberMe
) {}
