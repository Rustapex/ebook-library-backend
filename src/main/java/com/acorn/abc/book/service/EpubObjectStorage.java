package com.acorn.abc.book.service;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * EPUB 원본 binary object 저장소 계약.
 *
 * <p>구현체는 전체 file을 byte array로 적재하지 않고 stream으로 처리한다.
 */
public interface EpubObjectStorage {

    String CONTENT_TYPE = "application/epub+zip";

    /**
     * 저장된 object의 위치.
     *
     * <p>계약: {@code key}와 {@code versionId} 모두 blank일 수 없다.
     * bucket versioning은 배포 필수 조건이므로 {@link #put}은 versionId를 확보하지 못하면
     * DB metadata가 저장되기 전에 실패해야 한다.
     */
    record StoredEpubObject(String key, String versionId) {
    }

    /**
     * deterministic object key. 같은 도서/버전/내용이면 항상 같은 key가 나온다.
     */
    static String objectKey(Long bookId, int epubVersion, String sha256) {
        return "epubs/" + bookId + "/" + epubVersion + "/" + sha256 + ".epub";
    }

    /**
     * @return blank가 아닌 key와 versionId. versionId를 확보하지 못하면 구현체는 예외를 던진다.
     */
    StoredEpubObject put(Long bookId, int epubVersion, String sha256, long contentLength, InputStream input);

    void writeTo(String key, String versionId, OutputStream output);

    void delete(String key, String versionId);

    /**
     * bucket versioning 활성화 여부. 배포 preflight와 migration preflight에서 사용한다.
     * {@code s3:GetBucketVersioning} 권한이 필요하다.
     */
    boolean isVersioningEnabled();
}
