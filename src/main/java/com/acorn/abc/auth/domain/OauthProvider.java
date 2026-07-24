package com.acorn.abc.auth.domain;

/**
 * 외부 인증 제공자. DB의 {@code chk_oauth_account_provider} CHECK 제약과 값이 일치해야 한다.
 */
public enum OauthProvider {
    GOOGLE
}
