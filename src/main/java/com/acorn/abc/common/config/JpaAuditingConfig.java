package com.acorn.abc.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @EnableJpaAuditing을 메인 애플리케이션 클래스에서 분리한다.
 * 메인 클래스에 직접 선언하면 @WebMvcTest 등 JPA를 로드하지 않는 슬라이스 테스트에서도
 * jpaAuditingHandler/jpaMappingContext 빈 생성이 시도되어 "JPA metamodel must not be empty"로 실패한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
