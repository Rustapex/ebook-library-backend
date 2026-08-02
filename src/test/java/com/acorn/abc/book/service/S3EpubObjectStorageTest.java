package com.acorn.abc.book.service;

import com.acorn.abc.book.service.EpubObjectStorage.StoredEpubObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3EpubObjectStorageTest {

    private static final String BUCKET = "abc-epub-private";
    private static final String SHA256 = "a".repeat(64);
    private static final String EXPECTED_KEY = "epubs/7/3/" + SHA256 + ".epub";

    @Mock private S3Client s3Client;

    private S3EpubObjectStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3EpubObjectStorage(s3Client, BUCKET);
    }

    @Test
    void put_deterministic키와_contentType_contentLength로_업로드한다() {
        byte[] content = "EPUB-CONTENT".getBytes(StandardCharsets.UTF_8);
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().versionId("ver-1").build());

        StoredEpubObject stored = storage.put(7L, 3, SHA256, content.length,
                new ByteArrayInputStream(content));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getValue().contentType()).isEqualTo("application/epub+zip");
        assertThat(request.getValue().contentLength()).isEqualTo(content.length);
        assertThat(stored.key()).isEqualTo(EXPECTED_KEY);
        assertThat(stored.versionId()).isEqualTo("ver-1");
    }

    @Test
    void put_versionId가_null이면_DB_metadata를_만들지않고_실패한다() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        assertThatThrownBy(() -> storage.put(7L, 3, SHA256, 4L, new ByteArrayInputStream(new byte[4])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("versioning");
    }

    @Test
    void put_versionId가_공백문자열이면_실패한다() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().versionId("   ").build());

        assertThatThrownBy(() -> storage.put(7L, 3, SHA256, 4L, new ByteArrayInputStream(new byte[4])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void put_versionId가_S3의_null문자열이면_실패한다() {
        // versioning이 꺼진 bucket은 versionId를 "null" 문자열로 돌려주기도 하지만,
        // SDK는 이 경우 값을 채우지 않는다. 값이 비어 있으면 어떤 형태든 실패해야 한다.
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().versionId("").build());

        assertThatThrownBy(() -> storage.put(7L, 3, SHA256, 4L, new ByteArrayInputStream(new byte[4])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void writeTo_versionId가있으면_정확한version을_요청한다() {
        byte[] content = "EPUB-BYTES".getBytes(StandardCharsets.UTF_8);
        given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(responseStream(content));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        storage.writeTo(EXPECTED_KEY, "ver-1", output);

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getValue().versionId()).isEqualTo("ver-1");
        assertThat(output.toByteArray()).isEqualTo(content);
    }

    @Test
    void writeTo_versionId가없으면_key기준으로_요청한다() {
        given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(responseStream(new byte[3]));

        storage.writeTo(EXPECTED_KEY, null, new ByteArrayOutputStream());

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(request.capture());
        assertThat(request.getValue().versionId()).isNull();
    }

    @Test
    void delete_versionId가있으면_정확한version을_삭제한다() {
        storage.delete(EXPECTED_KEY, "ver-1");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getValue().versionId()).isEqualTo("ver-1");
    }

    @Test
    void delete_versionId가없으면_key기준으로_삭제한다() {
        storage.delete(EXPECTED_KEY, null);

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo(EXPECTED_KEY);
        assertThat(request.getValue().versionId()).isNull();
    }

    @Test
    void isVersioningEnabled는_ENABLED일때만_true다() {
        given(s3Client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
                .willReturn(GetBucketVersioningResponse.builder()
                        .status(BucketVersioningStatus.ENABLED).build());

        assertThat(storage.isVersioningEnabled()).isTrue();

        ArgumentCaptor<GetBucketVersioningRequest> request =
                ArgumentCaptor.forClass(GetBucketVersioningRequest.class);
        verify(s3Client).getBucketVersioning(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
    }

    @Test
    void isVersioningEnabled는_SUSPENDED나_미설정이면_false다() {
        given(s3Client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
                .willReturn(GetBucketVersioningResponse.builder()
                        .status(BucketVersioningStatus.SUSPENDED).build());
        assertThat(storage.isVersioningEnabled()).isFalse();

        given(s3Client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
                .willReturn(GetBucketVersioningResponse.builder().build());
        assertThat(storage.isVersioningEnabled()).isFalse();
    }

    @Test
    void 생성자_bucket이비어있으면_기동을실패시킨다() {
        assertThatThrownBy(() -> new S3EpubObjectStorage(s3Client, "  "))
                .isInstanceOf(IllegalStateException.class);
    }

    private ResponseInputStream<GetObjectResponse> responseStream(byte[] content) {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(content)));
    }
}
