package com.acorn.abc.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Google Identity Services가 발급한 ID token만 받는다.
 *
 * <p>email/name/sub를 함께 받지 않는다. 그 값들은 서버가 검증한 token의 claim에서만 얻는다.
 */
public record GoogleLoginRequest(

        @NotBlank(message = "Google 인증 정보가 필요합니다.")
        String idToken
) {}
