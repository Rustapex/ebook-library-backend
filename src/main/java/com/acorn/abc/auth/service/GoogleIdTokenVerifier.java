package com.acorn.abc.auth.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Google ID token 검증기.
 *
 * <p>signature, exp/nbf, issuer, audience는 주입받은 {@link JwtDecoder}가 검증한다
 * ({@code GoogleAuthConfig}에서 Spring Security 기본 timestamp validator를 유지한 채
 * issuer/audience validator를 추가로 구성한다). 이 클래스는 그 위에서 claim 수준의 계약만 본다.
 *
 * <p>decoder를 생성자로 주입받으므로 test는 Google network나 JWKS 조회 없이 local RSA key로 검증할 수 있다.
 *
 * <p>실패 원인은 서버 내부 분류(log)로만 남기고 외부 응답에는 노출하지 않는다. 어떤 검증이 실패했는지
 * 알려주면 공격자가 token을 조립하는 데 도움이 되기 때문이다. ID token 원문은 log에 남기지 않는다.
 */
public class GoogleIdTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_NAME = "name";

    private final JwtDecoder jwtDecoder;

    public GoogleIdTokenVerifier(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public GoogleIdentity verify(String idToken) {
        Jwt jwt = decode(idToken);

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            log.warn("Google ID token rejected: missing sub claim");
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
        }

        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null || email.isBlank()) {
            log.warn("Google ID token rejected: missing email claim (sub={})", subject);
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
        }

        if (!isEmailVerified(jwt)) {
            log.warn("Google ID token rejected: email_verified is not true (sub={})", subject);
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_EMAIL_NOT_VERIFIED);
        }

        return new GoogleIdentity(subject, email, jwt.getClaimAsString(CLAIM_NAME));
    }

    private Jwt decode(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
        }
        try {
            return jwtDecoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            // signature/exp/nbf/iss/aud 실패가 모두 여기로 온다. claim 형 변환 실패(IllegalArgumentException)도
            // 500이 아니라 인증 실패로 수렴시킨다. 원인은 내부 log로만 남긴다.
            log.warn("Google ID token rejected: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
        }
    }

    /**
     * Google은 email_verified를 boolean 또는 문자열("true")로 보낼 수 있다. 둘 다 처리하되
     * 명시적으로 true인 경우에만 통과시킨다(값이 없으면 거부).
     */
    private boolean isEmailVerified(Jwt jwt) {
        Object claim = jwt.getClaim(CLAIM_EMAIL_VERIFIED);
        if (claim instanceof Boolean booleanClaim) {
            return booleanClaim;
        }
        return claim instanceof String stringClaim && Boolean.parseBoolean(stringClaim);
    }
}
