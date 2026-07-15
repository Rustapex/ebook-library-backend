package com.acorn.abc.auth.dto;

import com.acorn.abc.auth.validation.ValidLoginId;

// 회원가입과 판정이 갈리지 않도록 SignupRequest.loginId와 동일한 @ValidLoginId 검증 계약을 공유한다.
public record CheckLoginIdRequest(

        @ValidLoginId
        String loginId
) {
}
