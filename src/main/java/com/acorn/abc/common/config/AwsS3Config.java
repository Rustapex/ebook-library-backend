package com.acorn.abc.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * EPUB S3 저장소 설정.
 *
 * <p>credential은 AWS default credential provider chain(EC2 Instance Profile 전제)만 사용한다.
 * access key / secret key property는 만들지 않는다.
 */
@Configuration
@ConditionalOnProperty(name = "app.epub.storage", havingValue = AwsS3Config.STORAGE_S3)
public class AwsS3Config {

    public static final String STORAGE_S3 = "s3";
    public static final String STORAGE_DB = "db";

    @Bean
    public S3Client epubS3Client(@Value("${app.aws.region:}") String region,
                                 @Value("${app.s3.epub-bucket:}") String bucket) {
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("app.aws.region(AWS_REGION) is required when app.epub.storage=s3");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.s3.epub-bucket(EPUB_BUCKET) is required when app.epub.storage=s3");
        }
        return S3Client.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
