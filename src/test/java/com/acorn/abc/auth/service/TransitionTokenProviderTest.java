package com.acorn.abc.auth.service;

import com.acorn.abc.auth.config.GoogleAuthProperties;
import com.acorn.abc.auth.domain.OauthProvider;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Google 로그인 구현 필수 test 14~15 — transition token 만료와 purpose 불일치.
 *
 * <p>추가로 access token과의 상호 격리를 검증한다. Google {@code sub}는 숫자 문자열이라 두 token이
 * 같은 key를 쓰면 transition token이 임의 회원의 access token으로 동작할 수 있다.
 */
class TransitionTokenProviderTest {

    private static final String JWT_SECRET = "test-jwt-secret-value-must-be-long-enough-for-hs256";
    private static final GoogleIdentity IDENTITY =
            new GoogleIdentity("109876543210987654321", "google.user@example.com", "구글사용자");

    private static TransitionTokenProvider providerWithExpiration(long expirationSeconds) {
        return new TransitionTokenProvider(JWT_SECRET,
                new GoogleAuthProperties(true, "client-id", expirationSeconds));
    }

    private final TransitionTokenProvider provider = providerWithExpiration(300);

    @Test
    void SIGNUP_token은_provider와_sub와_email을_보존한다() {
        String token = provider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);

        TransitionTokenClaims claims = provider.verify(token, TransitionTokenPurpose.SIGNUP);

        assertThat(claims.purpose()).isEqualTo(TransitionTokenPurpose.SIGNUP);
        assertThat(claims.provider()).isEqualTo(OauthProvider.GOOGLE);
        assertThat(claims.providerUserId()).isEqualTo(IDENTITY.subject());
        assertThat(claims.email()).isEqualTo(IDENTITY.email());
        assertThat(claims.name()).isEqualTo(IDENTITY.name());
        assertThat(claims.memberId()).isNull();
    }

    @Test
    void LINK_token은_연결_대상_memberId를_고정한다() {
        String token = provider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 42L);

        TransitionTokenClaims claims = provider.verify(token, TransitionTokenPurpose.LINK);

        assertThat(claims.purpose()).isEqualTo(TransitionTokenPurpose.LINK);
        assertThat(claims.memberId()).isEqualTo(42L);
    }

    @Test
    void SIGNUP_token으로_LINK를_수행할_수_없다() {
        String signupToken = provider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);

        assertThatThrownBy(() -> provider.verify(signupToken, TransitionTokenPurpose.LINK))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    @Test
    void LINK_token으로_SIGNUP을_수행할_수_없다() {
        String linkToken = provider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 42L);

        assertThatThrownBy(() -> provider.verify(linkToken, TransitionTokenPurpose.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    @Test
    void 만료된_transition_token은_거부한다() throws Exception {
        TransitionTokenProvider shortLived = providerWithExpiration(1);
        String token = shortLived.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);

        Thread.sleep(1200);

        assertThatThrownBy(() -> shortLived.verify(token, TransitionTokenPurpose.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    @Test
    void transition_token은_access_token으로_인증되지_않는다() {
        // sub가 숫자라 key가 같으면 memberId로 해석될 수 있다. 파생 key로 그 경로를 차단한다.
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 3600);
        String transitionToken = provider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);

        assertThat(jwtTokenProvider.validateToken(transitionToken)).isFalse();
    }

    @Test
    void access_token은_transition_token으로_사용할_수_없다() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 3600);
        String accessToken = jwtTokenProvider.generateToken(42L, "USER");

        assertThatThrownBy(() -> provider.verify(accessToken, TransitionTokenPurpose.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    @Test
    void 서명이_다른_transition_token은_거부한다() {
        TransitionTokenProvider otherServer = new TransitionTokenProvider(
                "completely-different-secret-value-for-another-server",
                new GoogleAuthProperties(true, "client-id", 300));
        String foreignToken = otherServer.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);

        assertThatThrownBy(() -> provider.verify(foreignToken, TransitionTokenPurpose.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    @Test
    void 비어있는_transition_token은_거부한다() {
        assertThatThrownBy(() -> provider.verify("  ", TransitionTokenPurpose.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }
}
