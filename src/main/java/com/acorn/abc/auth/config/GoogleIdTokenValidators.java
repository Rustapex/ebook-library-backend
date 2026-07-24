package com.acorn.abc.auth.config;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.List;
import java.util.Set;

/**
 * Google ID token 검증 규칙.
 *
 * <p>production({@link GoogleAuthConfig})과 test가 <b>같은</b> validator를 쓰도록 여기에 모은다.
 * test가 자체 validator를 조립하면 실제 배포되는 검증을 검증하지 못한다.
 */
public final class GoogleIdTokenValidators {

    /** Google은 iss를 두 형태 중 하나로 발급한다. 둘 다 유효하며 그 외는 거부한다. */
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private GoogleIdTokenValidators() {
    }

    /**
     * Spring Security 기본 validator(exp/nbf timestamp 등)를 유지한 채 issuer/audience를 추가한다.
     * {@code NimbusJwtDecoder.setJwtValidator}는 기존 validator를 <b>교체</b>하므로 기본값을 반드시 포함해야 한다.
     */
    public static OAuth2TokenValidator<Jwt> create(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuerValidator(),
                audienceValidator(clientId));
    }

    private static OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            // getIssuer()는 iss를 URL로 변환하므로 scheme 없는 "accounts.google.com"에서
            // IllegalArgumentException이 난다. Google은 두 형태를 모두 발급하므로 원시 claim을 문자열로 읽는다.
            String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            return GOOGLE_ISSUERS.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_issuer", "Google ID token issuer is not accepted", null));
        };
    }

    /**
     * aud가 우리 client ID와 정확히 일치해야 한다. 이 검증이 없으면 다른 애플리케이션용으로 발급된
     * 유효 서명 token을 그대로 재사용할 수 있다.
     */
    private static OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            return audience != null && audience.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_audience", "Google ID token audience is not accepted", null));
        };
    }
}
