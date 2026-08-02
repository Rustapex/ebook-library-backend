package com.acorn.abc.auth.config;

import com.acorn.abc.auth.service.GoogleIdTokenVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Google 로그인이 활성화된 경우에만 검증 구성 요소를 만든다.
 *
 * <p>{@code app.auth.google.enabled=false}면 이 configuration 자체가 생성되지 않으므로
 * {@link NimbusJwtDecoder}도 만들어지지 않고 JWKS network 요청도 발생하지 않는다.
 * client ID가 없는 환경에서도 애플리케이션이 정상 기동한다.
 *
 * <p>활성화 상태에서 client ID가 비어 있으면 audience를 검증할 수 없어 아무 Google 계정이나
 * 통과시키는 것과 같다. 그래서 기동 시점에 fail-fast한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.auth.google", name = "enabled", havingValue = "true")
public class GoogleAuthConfig {

    /**
     * Google ID token의 JWKS. signature 검증에 사용한다.
     * https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
     */
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(GoogleAuthProperties properties) {
        if (!properties.hasClientId()) {
            throw new IllegalStateException(
                    "app.auth.google.enabled=true 이지만 app.auth.google.client-id가 비어 있습니다. "
                            + "GOOGLE_CLIENT_ID를 설정하거나 GOOGLE_AUTH_ENABLED=false로 두십시오.");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(GoogleIdTokenValidators.create(properties.clientId()));

        return new GoogleIdTokenVerifier(decoder);
    }
}
