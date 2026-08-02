package com.acorn.abc.book.service;

import com.acorn.abc.common.config.AwsS3Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * private S3 bucket 기반 EPUB object 저장소.
 *
 * <p>signed URL, credential, secret은 로그에 남기지 않는다. 예외 메시지에는 bucket과 key만 포함한다.
 */
@Component
@ConditionalOnProperty(name = "app.epub.storage", havingValue = AwsS3Config.STORAGE_S3)
public class S3EpubObjectStorage implements EpubObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3EpubObjectStorage.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3EpubObjectStorage(S3Client epubS3Client, @Value("${app.s3.epub-bucket:}") String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.s3.epub-bucket(EPUB_BUCKET) is required when app.epub.storage=s3");
        }
        this.s3Client = epubS3Client;
        this.bucket = bucket.trim();
    }

    @Override
    public StoredEpubObject put(Long bookId, int epubVersion, String sha256, long contentLength, InputStream input) {
        String key = EpubObjectStorage.objectKey(bookId, epubVersion, sha256);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(CONTENT_TYPE)
                .contentLength(contentLength)
                .build();
        PutObjectResponse response = s3Client.putObject(request,
                RequestBody.fromInputStream(input, contentLength));

        // bucket versioning은 배포 필수 조건이다. versionId가 없으면 versioning이 꺼진 상태이며,
        // 이 상태로 DB metadata를 저장하면 이후 보상 삭제가 key 단위로 동작해
        // 성공한 다른 transaction의 object까지 지울 수 있다. metadata를 남기기 전에 실패시킨다.
        if (isBlank(response.versionId())) {
            log.error("EPUB S3 put returned no versionId (bucket versioning disabled). "
                            + "Object is orphaned and needs reconciliation: bucket={}, key={}",
                    bucket, key);
            throw new IllegalStateException(
                    "EPUB S3 put returned a blank versionId; bucket versioning must be enabled: "
                            + "bucket=" + bucket + ", key=" + key);
        }
        return new StoredEpubObject(key, response.versionId());
    }

    @Override
    public void writeTo(String key, String versionId, OutputStream output) {
        GetObjectRequest.Builder request = GetObjectRequest.builder().bucket(bucket).key(key);
        if (versionId != null && !versionId.isBlank()) {
            request.versionId(versionId);
        }
        try (ResponseInputStream<GetObjectResponse> input = s3Client.getObject(request.build())) {
            input.transferTo(output);
        } catch (IOException e) {
            throw new IllegalStateException("EPUB S3 stream write failed: bucket=" + bucket + ", key=" + key, e);
        }
    }

    @Override
    public void delete(String key, String versionId) {
        DeleteObjectRequest.Builder request = DeleteObjectRequest.builder().bucket(bucket).key(key);
        if (versionId != null && !versionId.isBlank()) {
            request.versionId(versionId);
        }
        s3Client.deleteObject(request.build());
    }

    @Override
    public boolean isVersioningEnabled() {
        GetBucketVersioningResponse response = s3Client.getBucketVersioning(
                GetBucketVersioningRequest.builder().bucket(bucket).build());
        return response.status() == BucketVersioningStatus.ENABLED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
