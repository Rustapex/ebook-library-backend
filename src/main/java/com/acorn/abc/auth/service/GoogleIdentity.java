package com.acorn.abc.auth.service;

/**
 * 검증이 끝난 Google ID token에서 추출한 신원.
 *
 * <p>frontend가 보낸 값이 아니라 서버가 signature를 검증한 token의 claim에서만 만든다.
 * {@code subject}는 Google의 {@code sub}이며 외부 계정 식별의 유일한 기준이다.
 */
public record GoogleIdentity(
        String subject,
        String email,
        String name
) {}
