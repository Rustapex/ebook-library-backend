package com.acorn.abc.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google 로그인 런타임 설정.
 *
 * <p>client secret은 이 기능에 필요하지 않으므로 property로 만들지 않는다. Google ID token의
 * audience 검증에 쓰는 client ID만 사용한다.
 *
 * <p>{@code enabled}가 false면 client ID가 비어 있어도 애플리케이션이 정상 기동해야 한다.
 * production 배포와 feature 활성화를 분리하기 위한 계약이다.
 */
@ConfigurationProperties(prefix = "app.auth.google")
public record GoogleAuthProperties(
        boolean enabled,
        String clientId,
        long transitionTokenExpiration
) {

    public GoogleAuthProperties {
        // 잘못된 값을 조용히 기본값으로 덮으면 오타가 그대로 배포된다. client-id 누락과 동일하게 기동을 막는다.
        if (transitionTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "app.auth.google.transition-token-expiration은 양수여야 합니다: " + transitionTokenExpiration);
        }
    }

    public boolean hasClientId() {
        return clientId != null && !clientId.isBlank();
    }
}
