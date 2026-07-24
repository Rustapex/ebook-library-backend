package com.acorn.abc.book.service;

import com.acorn.abc.book.service.EpubObjectStorage.StoredEpubObject;
import com.acorn.abc.common.config.AwsS3Config;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EPUB binary 저장/조회. {@code app.epub.storage} 값에 따라 DB LONGBLOB 또는 S3 object를 사용한다.
 *
 * <p>S3 mode에서 신규 upload row는 metadata만 DB에 남기고 {@code epub_data}는 NULL이다.
 * S3 upload 후 DB 작업이 실패하거나 transaction이 rollback되면 업로드한 object version을 보상 삭제한다.
 */
@Component
public class BookEpubBinaryStore {

    public static final String STORAGE_TYPE_DB = "DB";
    public static final String STORAGE_TYPE_S3 = "S3";

    private static final Logger log = LoggerFactory.getLogger(BookEpubBinaryStore.class);

    private static final String SELECT_LOCKED_BOOK = "SELECT book_id FROM book WHERE book_id = ? FOR UPDATE";
    private static final String SELECT_NEXT_VERSION =
            "SELECT COALESCE(MAX(epub_version), 0) + 1 FROM book_epub WHERE book_id = ?";
    private static final String INSERT_DB_ROW = "INSERT INTO book_epub "
            + "(book_id, original_file_name, content_type, file_size, sha256, epub_version, epub_data, "
            + "storage_type, storage_key, s3_version_id, created_at) "
            + "VALUES (?, ?, 'application/epub+zip', ?, ?, ?, ?, 'DB', NULL, NULL, NOW())";
    private static final String INSERT_S3_ROW = "INSERT INTO book_epub "
            + "(book_id, original_file_name, content_type, file_size, sha256, epub_version, epub_data, "
            + "storage_type, storage_key, s3_version_id, created_at) "
            + "VALUES (?, ?, 'application/epub+zip', ?, ?, ?, NULL, 'S3', ?, ?, NOW())";
    private static final String SELECT_LOCATION =
            "SELECT storage_type, storage_key, s3_version_id FROM book_epub "
                    + "WHERE book_id = ? AND epub_version = ?";
    private static final String SELECT_BLOB =
            "SELECT epub_data FROM book_epub WHERE book_id = ? AND epub_version = ?";

    private final JdbcTemplate jdbcTemplate;
    private final EpubObjectStorage objectStorage;
    private final boolean s3Mode;

    public BookEpubBinaryStore(JdbcTemplate jdbcTemplate,
                               Optional<EpubObjectStorage> objectStorage,
                               @Value("${app.epub.storage:db}") String storageMode) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectStorage = objectStorage.orElse(null);
        this.s3Mode = AwsS3Config.STORAGE_S3.equalsIgnoreCase(storageMode == null ? "" : storageMode.trim());
        if (this.s3Mode && this.objectStorage == null) {
            throw new IllegalStateException("app.epub.storage=s3 requires an EpubObjectStorage bean");
        }
    }

    public int insertNextVersion(Long bookId, String fileName, long fileSize, String sha256, MultipartFile file) {
        int nextVersion = lockBookAndResolveNextVersion(bookId);
        if (s3Mode) {
            insertS3Version(bookId, fileName, fileSize, sha256, file, nextVersion);
        } else {
            insertDbVersion(bookId, fileName, fileSize, sha256, file, nextVersion);
        }
        return nextVersion;
    }

    public void writeTo(Long bookId, int epubVersion, OutputStream output) {
        List<EpubLocation> locations = jdbcTemplate.query(SELECT_LOCATION,
                (resultSet, rowNum) -> new EpubLocation(resultSet.getString(1), resultSet.getString(2),
                        resultSet.getString(3)),
                bookId, epubVersion);
        if (locations.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND);
        }

        EpubLocation location = locations.get(0);
        String storageType = normalizeStorageType(location.storageType());
        if (STORAGE_TYPE_S3.equals(storageType)) {
            writeFromS3(bookId, epubVersion, location, output);
        } else if (STORAGE_TYPE_DB.equals(storageType)) {
            writeFromDb(bookId, epubVersion, output);
        } else {
            // 알 수 없는 값을 DB fallback으로 처리하면 손상된 metadata를 조용히 넘긴다. 명시적으로 실패시킨다.
            throw new IllegalStateException("Unknown book_epub.storage_type='" + location.storageType()
                    + "': bookId=" + bookId + ", epubVersion=" + epubVersion);
        }
    }

    private int lockBookAndResolveNextVersion(Long bookId) {
        List<Long> lockedBooks = jdbcTemplate.query(SELECT_LOCKED_BOOK,
                (resultSet, rowNum) -> resultSet.getLong(1), bookId);
        if (lockedBooks.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        Integer nextVersion = jdbcTemplate.queryForObject(SELECT_NEXT_VERSION, Integer.class, bookId);
        if (nextVersion == null) {
            throw new IllegalStateException("EPUB version calculation failed");
        }
        return nextVersion;
    }

    private void insertDbVersion(Long bookId, String fileName, long fileSize, String sha256,
                                 MultipartFile file, int nextVersion) {
        try (InputStream input = file.getInputStream()) {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(INSERT_DB_ROW);
                statement.setLong(1, bookId);
                statement.setString(2, fileName);
                statement.setLong(3, fileSize);
                statement.setString(4, sha256);
                statement.setInt(5, nextVersion);
                statement.setBinaryStream(6, input, fileSize);
                return statement;
            });
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
    }

    private void insertS3Version(Long bookId, String fileName, long fileSize, String sha256,
                                 MultipartFile file, int nextVersion) {
        StoredEpubObject stored;
        try (InputStream input = file.getInputStream()) {
            stored = objectStorage.put(bookId, nextVersion, sha256, fileSize, input);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }

        CompensatingDelete compensation = new CompensatingDelete(objectStorage, stored);
        registerRollbackCompensation(compensation);
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(INSERT_S3_ROW);
                statement.setLong(1, bookId);
                statement.setString(2, fileName);
                statement.setLong(3, fileSize);
                statement.setString(4, sha256);
                statement.setInt(5, nextVersion);
                statement.setString(6, stored.key());
                statement.setString(7, stored.versionId());
                return statement;
            });
        } catch (RuntimeException e) {
            compensateAfterDbFailure(compensation, e);
            throw e;
        }
    }

    /**
     * DB 실패 직후의 즉시 보상 삭제.
     *
     * <p>삭제가 실패해도 원래 DB 예외를 가리지 않는다. 삭제 실패는 suppressed 예외와 로그로 남기고,
     * 완료 상태를 설정하지 않으므로 {@code afterCompletion} 또는 reconciliation에서 다시 시도한다.
     */
    private void compensateAfterDbFailure(CompensatingDelete compensation, RuntimeException original) {
        try {
            compensation.run();
        } catch (RuntimeException deleteFailure) {
            original.addSuppressed(deleteFailure);
            log.error("EPUB S3 compensating delete failed after DB failure, retry pending: key={}, versionId={}",
                    compensation.key(), compensation.versionId(), deleteFailure);
        }
    }

    private void registerRollbackCompensation(CompensatingDelete compensation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    return;
                }
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                    // STATUS_UNKNOWN: commit 여부를 알 수 없다. 지우면 commit된 row의 object를
                    // 삭제할 수 있으므로 삭제하지 않고 reconciliation 대상으로 남긴다.
                    log.error("EPUB S3 transaction outcome unknown, object left for reconciliation: "
                            + "key={}, versionId={}", compensation.key(), compensation.versionId());
                    return;
                }
                try {
                    compensation.run();
                } catch (RuntimeException e) {
                    // afterCompletion에서 던지면 다른 synchronization을 방해한다. orphan으로 기록만 한다.
                    log.error("EPUB S3 compensating delete failed after rollback, object is orphaned: "
                            + "key={}, versionId={}", compensation.key(), compensation.versionId(), e);
                }
            }
        });
    }

    private void writeFromS3(Long bookId, int epubVersion, EpubLocation location, OutputStream output) {
        // metadata가 깨진 S3 row를 DB blob으로 조용히 fallback하지 않는다.
        // S3 row는 storage_key와 s3_version_id가 모두 non-blank여야 한다(V003, migration preflight와 동일 계약).
        // versionId 없이 읽으면 "최신 version"을 읽게 되는데, 이는 요청한 것과 다른 내용을 조용히
        // 돌려줄 수 있는 fallback이므로 허용하지 않는다.
        if (isBlank(location.storageKey())) {
            throw new IllegalStateException("EPUB S3 metadata is invalid, storage_key is blank: bookId=" + bookId
                    + ", epubVersion=" + epubVersion);
        }
        if (isBlank(location.s3VersionId())) {
            throw new IllegalStateException("EPUB S3 metadata is invalid, s3_version_id is blank: bookId=" + bookId
                    + ", epubVersion=" + epubVersion);
        }
        if (objectStorage == null) {
            throw new IllegalStateException("EPUB S3 row requires an EpubObjectStorage bean: bookId=" + bookId
                    + ", epubVersion=" + epubVersion);
        }
        objectStorage.writeTo(location.storageKey(), location.s3VersionId(), output);
    }

    private void writeFromDb(Long bookId, int epubVersion, OutputStream output) {
        Integer rows = jdbcTemplate.query(SELECT_BLOB, resultSet -> {
            if (!resultSet.next()) {
                return 0;
            }
            try (InputStream input = resultSet.getBinaryStream(1)) {
                if (input == null) {
                    throw new IllegalStateException("EPUB DB blob is missing: bookId=" + bookId
                            + ", epubVersion=" + epubVersion);
                }
                input.transferTo(output);
            } catch (IOException e) {
                throw new IllegalStateException("EPUB stream write failed", e);
            }
            return 1;
        }, bookId, epubVersion);
        if (rows == null || rows == 0) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND);
        }
    }

    private static String normalizeStorageType(String storageType) {
        return storageType == null ? "" : storageType.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record EpubLocation(String storageType, String storageKey, String s3VersionId) {
    }

    /**
     * 보상 삭제. 삭제에 <b>성공한 뒤에만</b> 완료 상태를 설정하므로,
     * 실패한 삭제는 이후 afterCompletion이나 reconciliation에서 다시 시도할 수 있다.
     */
    private static final class CompensatingDelete {

        private final EpubObjectStorage objectStorage;
        private final StoredEpubObject stored;
        private final AtomicBoolean deleted = new AtomicBoolean(false);

        private CompensatingDelete(EpubObjectStorage objectStorage, StoredEpubObject stored) {
            this.objectStorage = objectStorage;
            this.stored = stored;
        }

        private String key() {
            return stored.key();
        }

        private String versionId() {
            return stored.versionId();
        }

        private void run() {
            if (deleted.get()) {
                return;
            }
            objectStorage.delete(stored.key(), stored.versionId());
            deleted.set(true);
        }
    }
}
