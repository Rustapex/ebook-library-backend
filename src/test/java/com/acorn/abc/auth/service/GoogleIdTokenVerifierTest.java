package com.acorn.abc.auth.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static com.acorn.abc.auth.service.GoogleIdTokenTestSupport.EMAIL;
import static com.acorn.abc.auth.service.GoogleIdTokenTestSupport.NAME;
import static com.acorn.abc.auth.service.GoogleIdTokenTestSupport.SUBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Google 로그인 구현 필수 test 9~13 — Google ID token 검증.
 *
 * <p>production과 같은 validator 구성을 쓰되 local RSA key로만 서명/검증하므로 외부 network에 의존하지 않는다.
 */
class GoogleIdTokenVerifierTest {

    private static KeyPair googleKeyPair;
    private static KeyPair attackerKeyPair;
    private static GoogleIdTokenVerifier verifier;

    @BeforeAll
    static void setUp() {
        googleKeyPair = GoogleIdTokenTestSupport.generateKeyPair();
        attackerKeyPair = GoogleIdTokenTestSupport.generateKeyPair();
        verifier = GoogleIdTokenTestSupport.verifierFor(googleKeyPair);
    }

    @Test
    void 정상_token이면_sub_email_name을_추출한다() {
        String token = GoogleIdTokenTestSupport.validToken(googleKeyPair.getPrivate());

        GoogleIdentity identity = verifier.verify(token);

        assertThat(identity.subject()).isEqualTo(SUBJECT);
        assertThat(identity.email()).isEqualTo(EMAIL);
        assertThat(identity.name()).isEqualTo(NAME);
    }

    @Test
    void issuer가_accounts_google_com_형태여도_통과한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.issuer("accounts.google.com"));

        assertThat(verifier.verify(token).subject()).isEqualTo(SUBJECT);
    }

    @Test
    void 잘못된_signature는_거부한다() {
        // Google이 아닌 key로 서명한 token. claim 자체는 모두 정상이다.
        String token = GoogleIdTokenTestSupport.validToken(attackerKeyPair.getPrivate());

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }

    @Test
    void 잘못된_audience는_거부한다() {
        // 다른 애플리케이션용으로 발급된 유효 서명 token의 재사용을 막는다.
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.audience("another-app.apps.googleusercontent.com"));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }

    @Test
    void 잘못된_issuer는_거부한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.issuer("https://evil.example.com"));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }

    @Test
    void 만료된_token은_거부한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.expiresInSeconds(-120));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }

    @Test
    void email_verified가_false면_거부한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.claim("email_verified", Boolean.FALSE));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_EMAIL_NOT_VERIFIED);
    }

    @Test
    void email_verified_claim이_없으면_거부한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.removeClaim("email_verified"));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_EMAIL_NOT_VERIFIED);
    }

    @Test
    void email_verified가_문자열_true여도_통과한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.claim("email_verified", "true"));

        assertThat(verifier.verify(token).subject()).isEqualTo(SUBJECT);
    }

    @Test
    void email_claim이_없으면_거부한다() {
        String token = GoogleIdTokenTestSupport.token(googleKeyPair.getPrivate(),
                spec -> spec.removeClaim("email"));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }

    @Test
    void 비어있는_token은_거부한다() {
        assertThatThrownBy(() -> verifier.verify("  "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }
}
