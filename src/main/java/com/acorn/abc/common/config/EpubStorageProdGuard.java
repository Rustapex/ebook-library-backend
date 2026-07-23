package com.acorn.abc.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * prod profile에서 EPUB 저장소 설정이 잘못되면 기동을 실패시킨다.
 *
 * <p>prod는 S3 저장소만 허용한다. 값이 잘못되면 DB LONGBLOB으로 조용히 fallback하지 않는다.
 *
 * <p>{@link BeanFactoryPostProcessor}로 동작하므로 DataSource/EntityManagerFactory보다 먼저 검증한다.
 * 그렇지 않으면 DB 연결 실패가 설정 오류를 가려서 원인 파악이 어려워진다.
 */
@Component
@Profile("prod")
public class EpubStorageProdGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        validate(environment.getProperty("app.epub.storage"),
                environment.getProperty("app.s3.epub-bucket"),
                environment.getProperty("app.aws.region"));
    }

    static void validate(String storage, String bucket, String region) {
        if (!AwsS3Config.STORAGE_S3.equalsIgnoreCase(trim(storage))) {
            throw new IllegalStateException(
                    "prod profile requires app.epub.storage(EPUB_STORAGE)=s3 but was: " + trim(storage));
        }
        if (trim(bucket).isEmpty()) {
            throw new IllegalStateException("prod profile requires app.s3.epub-bucket(EPUB_BUCKET)");
        }
        if (trim(region).isEmpty()) {
            throw new IllegalStateException("prod profile requires app.aws.region(AWS_REGION)");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
