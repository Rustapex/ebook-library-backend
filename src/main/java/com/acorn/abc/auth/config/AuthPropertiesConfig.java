package com.acorn.abc.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Google 로그인 설정 binding은 feature flag와 무관하게 항상 이뤄져야 한다.
 * 비활성 상태를 판단하려면 {@code enabled=false} 값 자체를 읽을 수 있어야 하기 때문이다.
 *
 * <p>검증 구성 요소(JwtDecoder 등)는 {@link GoogleAuthConfig}에서 활성화된 경우에만 만든다.
 */
@Configuration
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class AuthPropertiesConfig {
}
