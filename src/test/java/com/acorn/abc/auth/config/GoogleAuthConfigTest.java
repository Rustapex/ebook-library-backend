package com.acorn.abc.auth.config;

import com.acorn.abc.auth.service.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * feature flag의 기동 계약을 검증한다. 이 동작은 production 배포 안전성의 전제다.
 *
 * <ul>
 *   <li>flag가 꺼져 있으면 GOOGLE_CLIENT_ID가 없어도 기동해야 한다. 그렇지 않으면 .env.prod에
 *       값을 넣기 전에 배포하는 순간 container가 죽어 장애가 된다.</li>
 *   <li>flag가 꺼져 있으면 NimbusJwtDecoder를 만들지 않는다(JWKS 요청 없음).</li>
 *   <li>flag가 켜져 있는데 client ID가 비면 audience를 검증할 수 없어 아무 Google 계정이나
 *       통과시키는 것과 같다. 조용히 동작하지 말고 기동을 막아야 한다.</li>
 * </ul>
 */
class GoogleAuthConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(AuthPropertiesConfig.class, GoogleAuthConfig.class);

    @Test
    void flag가_꺼져_있으면_client_ID가_없어도_기동하고_verifier를_만들지_않는다() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.enabled=false",
                        "app.auth.google.client-id=",
                        "app.auth.google.transition-token-expiration=300")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(GoogleIdTokenVerifier.class);
                });
    }

    @Test
    void flag_property가_아예_없어도_기동하고_verifier를_만들지_않는다() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.client-id=",
                        "app.auth.google.transition-token-expiration=300")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(GoogleIdTokenVerifier.class);
                });
    }

    @Test
    void flag가_켜져_있고_client_ID가_있으면_verifier를_만든다() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.enabled=true",
                        "app.auth.google.client-id=test-client-id.apps.googleusercontent.com",
                        "app.auth.google.transition-token-expiration=300")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GoogleIdTokenVerifier.class);
                });
    }

    @Test
    void flag가_켜져_있는데_client_ID가_비면_기동에_실패한다() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.enabled=true",
                        "app.auth.google.client-id=",
                        "app.auth.google.transition-token-expiration=300")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("client-id"));
    }

    @Test
    void transition_token_수명이_양수가_아니면_기동에_실패한다() {
        contextRunner
                .withPropertyValues(
                        "app.auth.google.enabled=true",
                        "app.auth.google.client-id=test-client-id.apps.googleusercontent.com",
                        "app.auth.google.transition-token-expiration=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
