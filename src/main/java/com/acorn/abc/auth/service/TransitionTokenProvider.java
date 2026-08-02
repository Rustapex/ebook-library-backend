package com.acorn.abc.auth.service;

import com.acorn.abc.auth.config.GoogleAuthProperties;
import com.acorn.abc.auth.domain.OauthProvider;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Google 신규 가입/계정 연결 절차에서만 쓰는 단기 token 발급/검증.
 *
 * <p>access token과의 격리가 이 클래스의 핵심 보안 요건이다. Google {@code sub}는 숫자 문자열이라
 * 만약 같은 key로 서명하면 {@code JwtAuthenticationFilter}가 subject를 memberId로 해석해 임의 회원으로
 * 인증될 수 있다. 그래서 {@code jwt.secret}에서 파생한 <b>별도 key</b>로 서명한다. 파생 key로 서명된
 * token은 {@code JwtTokenProvider}의 검증을 통과하지 못하고, 반대로 access token도 여기서 거부된다.
 *
 * <p>추가로 {@code purpose} claim을 검증해 SIGNUP token으로 link를, LINK token으로 signup을 할 수 없게 한다.
 *
 * <p>token 원문은 log에 남기지 않는다.
 */
@Component
public class TransitionTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TransitionTokenProvider.class);

    /** access token key와 절대 같아지지 않도록 고정 도메인 문자열을 섞어 파생한다. */
    private static final String KEY_DERIVATION_CONTEXT = "|abc-google-transition-token-v1";

    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_PROVIDER = "provider";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_MEMBER_ID = "memberId";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public TransitionTokenProvider(@Value("${jwt.secret}") String secret,
                                   GoogleAuthProperties googleAuthProperties) {
        this.signingKey = deriveKey(secret);
        this.expirationSeconds = googleAuthProperties.transitionTokenExpiration();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String issueSignupToken(OauthProvider provider, GoogleIdentity identity) {
        return issue(TransitionTokenPurpose.SIGNUP, provider, identity, null);
    }

    public String issueLinkToken(OauthProvider provider, GoogleIdentity identity, Long memberId) {
        return issue(TransitionTokenPurpose.LINK, provider, identity, memberId);
    }

    /**
     * @param expectedPurpose 이 endpoint가 허용하는 목적. 불일치하면 거부한다.
     */
    public TransitionTokenClaims verify(String token, TransitionTokenPurpose expectedPurpose) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치(access token 포함)와 만료가 모두 여기로 온다.
            // jjwt의 MalformedJwtException 메시지에는 공격자가 보낸 payload 조각(개행 포함)이 섞일 수 있어
            // log injection이 된다. 메시지 대신 예외 타입만 남긴다.
            log.warn("transition token rejected: {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }

        TransitionTokenPurpose purpose = parsePurpose(claims.get(CLAIM_PURPOSE, String.class));
        if (purpose != expectedPurpose) {
            log.warn("transition token rejected: purpose mismatch (expected={})", expectedPurpose);
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }

        OauthProvider provider = parseProvider(claims.get(CLAIM_PROVIDER, String.class));
        String providerUserId = claims.getSubject();
        String email = claims.get(CLAIM_EMAIL, String.class);
        if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }

        Long memberId = claims.get(CLAIM_MEMBER_ID, Long.class);
        if (expectedPurpose == TransitionTokenPurpose.LINK && memberId == null) {
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }

        return new TransitionTokenClaims(
                purpose, provider, providerUserId, email, claims.get(CLAIM_NAME, String.class), memberId);
    }

    private String issue(TransitionTokenPurpose purpose, OauthProvider provider,
                         GoogleIdentity identity, Long memberId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(identity.subject())
                .claim(CLAIM_PURPOSE, purpose.name())
                .claim(CLAIM_PROVIDER, provider.name())
                .claim(CLAIM_EMAIL, identity.email())
                .claim(CLAIM_NAME, identity.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationSeconds * 1000));

        if (memberId != null) {
            builder.claim(CLAIM_MEMBER_ID, memberId);
        }

        return builder.signWith(signingKey).compact();
    }

    private TransitionTokenPurpose parsePurpose(String value) {
        try {
            return TransitionTokenPurpose.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }
    }

    private OauthProvider parseProvider(String value) {
        try {
            return OauthProvider.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
        }
    }

    /**
     * access token key와의 충돌을 구조적으로 배제하기 위한 파생. 새 환경변수를 만들지 않으면서도
     * 두 token 계열이 서로의 서명을 검증할 수 없게 한다.
     */
    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] derived = digest.digest(
                    (secret + KEY_DERIVATION_CONTEXT).getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(derived);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for transition token key derivation", e);
        }
    }
}
