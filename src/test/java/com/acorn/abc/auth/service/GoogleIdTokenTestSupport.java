package com.acorn.abc.auth.service;

import com.acorn.abc.auth.config.GoogleIdTokenValidators;
import io.jsonwebtoken.Jwts;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Google ID token test 지원.
 *
 * <p>local RSA key로 token을 만들고 같은 key로 검증한다. Google network나 JWKS 조회에 의존하지 않는다.
 * decoder는 production과 <b>동일한</b> {@link GoogleIdTokenValidators}를 사용하므로 실제 배포되는
 * issuer/audience/timestamp 검증을 그대로 검증한다.
 */
final class GoogleIdTokenTestSupport {

    static final String CLIENT_ID = "test-google-client-id.apps.googleusercontent.com";
    static final String GOOGLE_ISSUER = "https://accounts.google.com";
    static final String SUBJECT = "109876543210987654321";
    static final String EMAIL = "google.user@example.com";
    static final String NAME = "구글사용자";

    private GoogleIdTokenTestSupport() {
    }

    static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static JwtDecoder decoderFor(KeyPair keyPair, String clientId) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(GoogleIdTokenValidators.create(clientId));
        return decoder;
    }

    static GoogleIdTokenVerifier verifierFor(KeyPair keyPair) {
        return new GoogleIdTokenVerifier(decoderFor(keyPair, CLIENT_ID));
    }

    /** 모든 검증을 통과하는 기본 token. */
    static String validToken(PrivateKey privateKey) {
        return token(privateKey, builder -> {
        });
    }

    static String token(PrivateKey privateKey, java.util.function.Consumer<TokenSpec> customizer) {
        TokenSpec spec = new TokenSpec();
        customizer.accept(spec);

        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(spec.issuer)
                .subject(spec.subject)
                .audience().add(spec.audience).and()
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(spec.expiresInSeconds)));

        for (Map.Entry<String, Object> claim : spec.claims.entrySet()) {
            if (claim.getValue() != null) {
                builder.claim(claim.getKey(), claim.getValue());
            }
        }
        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    static final class TokenSpec {
        private String issuer = GOOGLE_ISSUER;
        private String subject = SUBJECT;
        private String audience = CLIENT_ID;
        private long expiresInSeconds = 3600;
        private final java.util.LinkedHashMap<String, Object> claims = new java.util.LinkedHashMap<>();

        TokenSpec() {
            claims.put("email", EMAIL);
            claims.put("email_verified", Boolean.TRUE);
            claims.put("name", NAME);
        }

        TokenSpec issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        TokenSpec subject(String subject) {
            this.subject = subject;
            return this;
        }

        TokenSpec audience(String audience) {
            this.audience = audience;
            return this;
        }

        /** 음수를 주면 이미 만료된 token이 된다. */
        TokenSpec expiresInSeconds(long expiresInSeconds) {
            this.expiresInSeconds = expiresInSeconds;
            return this;
        }

        TokenSpec claim(String name, Object value) {
            claims.put(name, value);
            return this;
        }

        TokenSpec removeClaim(String name) {
            claims.remove(name);
            return this;
        }
    }
}
