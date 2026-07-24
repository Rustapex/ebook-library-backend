package com.acorn.abc.book.service;

import com.acorn.abc.book.service.EpubObjectStorage.StoredEpubObject;
import com.acorn.abc.common.config.AwsS3Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

/**
 * 기존 DB LONGBLOB EPUB을 S3로 옮기는 one-off migration.
 *
 * <p>명시적 opt-in에서만 실행되며 application 기동 시 자동 실행되지 않는다.
 * 성공한 row도 시연 rollback을 위해 기존 {@code epub_data}를 그대로 유지한다.
 *
 * <p>실행 전 preflight로 (1) 단일 실행 lock, (2) 전체 {@code book_epub} row invariant,
 * (3) bucket versioning 활성화를 확인한다. 하나라도 실패하면 어떤 row도 변경하지 않는다.
 */
@Service
@ConditionalOnProperty(name = "app.epub.storage", havingValue = AwsS3Config.STORAGE_S3)
public class EpubS3MigrationService {

    private static final Logger log = LoggerFactory.getLogger(EpubS3MigrationService.class);

    /** MySQL advisory lock 이름. 동시에 두 개의 runner가 도는 것을 막는다. */
    static final String MIGRATION_LOCK_NAME = "abc_epub_s3_migration";

    private static final String ACQUIRE_LOCK = "SELECT GET_LOCK(?, 0)";
    private static final String RELEASE_LOCK = "SELECT RELEASE_LOCK(?)";

    private static final int MIN_BATCH_SIZE = 1;
    private static final int MAX_BATCH_SIZE = 500;

    private static final String COUNT_BY_STORAGE_TYPE =
            "SELECT COUNT(*) FROM book_epub WHERE storage_type = ?";
    // V003의 preflight/postflight invariant와 같은 계약을 사용한다.
    // S3 row는 storage_key와 s3_version_id가 모두 non-blank여야 한다(공백 문자열도 위반).
    private static final String CHECK_INVARIANTS = "SELECT "
            + "COALESCE(SUM(storage_type IS NULL OR storage_type NOT IN ('DB', 'S3')), 0), "
            + "COALESCE(SUM(storage_type = 'DB' AND epub_data IS NULL), 0), "
            + "COALESCE(SUM(storage_type = 'DB' AND storage_key IS NOT NULL), 0), "
            + "COALESCE(SUM(storage_type = 'S3' AND (storage_key IS NULL OR TRIM(storage_key) = '')), 0), "
            + "COALESCE(SUM(storage_type = 'S3' AND (s3_version_id IS NULL OR TRIM(s3_version_id) = '')), 0) "
            + "FROM book_epub";
    private static final String SELECT_BATCH =
            "SELECT book_epub_id, book_id, epub_version, file_size, sha256 FROM book_epub "
                    + "WHERE storage_type = 'DB' AND book_epub_id > ? ORDER BY book_epub_id LIMIT ?";
    private static final String SELECT_BLOB =
            "SELECT epub_data FROM book_epub WHERE book_epub_id = ?";
    private static final String UPDATE_TO_S3 =
            "UPDATE book_epub SET storage_type = 'S3', storage_key = ?, s3_version_id = ? "
                    + "WHERE book_epub_id = ? AND storage_type = 'DB'";

    private final JdbcTemplate jdbcTemplate;
    private final EpubObjectStorage objectStorage;

    public EpubS3MigrationService(JdbcTemplate jdbcTemplate, EpubObjectStorage objectStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectStorage = objectStorage;
    }

    /** migration 결과 요약. key, URL, credential은 담지 않는다. */
    public record MigrationReport(long candidates, long migrated, long skipped, long failed,
                                  long invariantViolations) {
    }

    /** 전체 {@code book_epub} row의 저장소 invariant 위반 건수. */
    public record InvariantViolations(long unknownStorageType, long dbMissingBlob,
                                      long dbWithStorageKey, long s3MissingKey,
                                      long s3MissingVersionId) {

        public long total() {
            return unknownStorageType + dbMissingBlob + dbWithStorageKey + s3MissingKey + s3MissingVersionId;
        }
    }

    /**
     * @param dryRun    true면 DB와 S3를 전혀 변경하지 않고 대상 건수만 센다.
     *                  preflight(invariant, versioning)는 dry-run에서도 그대로 수행한다.
     * @param batchSize 한 번에 읽을 row 수.
     */
    public MigrationReport migrate(boolean dryRun, int batchSize) {
        return withSingleRunLock(() -> runMigration(dryRun, batchSize));
    }

    private MigrationReport runMigration(boolean dryRun, int batchSize) {
        int size = Math.max(MIN_BATCH_SIZE, Math.min(MAX_BATCH_SIZE, batchSize));
        long candidates = count("DB");
        long alreadyOnS3 = count("S3");

        // preflight 1: 전체 row invariant. 손상 row가 있으면 dry-run이든 아니든 아무것도 옮기지 않는다.
        InvariantViolations violations = checkInvariants();
        if (violations.total() > 0) {
            log.error("EPUB S3 migration aborted by invariant preflight: unknownStorageType={}, "
                            + "dbMissingBlob={}, dbWithStorageKey={}, s3MissingKey={}, s3MissingVersionId={}",
                    violations.unknownStorageType(), violations.dbMissingBlob(),
                    violations.dbWithStorageKey(), violations.s3MissingKey(),
                    violations.s3MissingVersionId());
            return new MigrationReport(candidates, 0, alreadyOnS3, violations.total(), violations.total());
        }

        // preflight 2: bucket versioning은 배포 필수 조건이다. 꺼져 있으면 보상 삭제가 이전 object를
        // 복구 불가능하게 지울 수 있으므로 진행하지 않는다.
        if (!objectStorage.isVersioningEnabled()) {
            throw new IllegalStateException(
                    "EPUB bucket versioning must be enabled before running the S3 migration");
        }

        if (dryRun) {
            log.info("EPUB S3 migration dry-run: candidates={}, alreadyOnS3={}, invariantViolations=0",
                    candidates, alreadyOnS3);
            return new MigrationReport(candidates, 0, alreadyOnS3, 0, 0);
        }

        long migrated = 0;
        long failed = 0;
        long cursor = 0;
        while (true) {
            List<EpubRow> rows = jdbcTemplate.query(SELECT_BATCH,
                    (resultSet, rowNum) -> new EpubRow(
                            resultSet.getLong(1), resultSet.getLong(2), resultSet.getInt(3),
                            resultSet.getLong(4), resultSet.getString(5)),
                    cursor, size);
            if (rows.isEmpty()) {
                break;
            }
            for (EpubRow row : rows) {
                cursor = Math.max(cursor, row.bookEpubId());
                if (migrateRow(row)) {
                    migrated++;
                } else {
                    failed++;
                }
            }
        }

        log.info("EPUB S3 migration finished: candidates={}, migrated={}, skipped={}, failed={}",
                candidates, migrated, alreadyOnS3, failed);
        return new MigrationReport(candidates, migrated, alreadyOnS3, failed, 0);
    }

    /**
     * 전체 {@code book_epub} row가 저장소 invariant를 지키는지 확인한다.
     *
     * <pre>
     * DB row: epub_data NOT NULL, storage_key NULL
     * S3 row: storage_key NOT NULL이고 blank 아님, s3_version_id NOT NULL이고 blank 아님
     * storage_type은 DB 또는 S3만 허용
     * </pre>
     */
    public InvariantViolations checkInvariants() {
        List<InvariantViolations> rows = jdbcTemplate.query(CHECK_INVARIANTS,
                (resultSet, rowNum) -> new InvariantViolations(resultSet.getLong(1), resultSet.getLong(2),
                        resultSet.getLong(3), resultSet.getLong(4), resultSet.getLong(5)));
        return rows.isEmpty() ? new InvariantViolations(0, 0, 0, 0, 0) : rows.get(0);
    }

    /**
     * 단일 실행 guard. MySQL {@code GET_LOCK}은 connection 단위이므로 전용 connection 하나를
     * migration이 끝날 때까지 붙잡아 둔다. 두 번째 runner는 즉시 실패한다.
     */
    private MigrationReport withSingleRunLock(Supplier<MigrationReport> action) {
        return jdbcTemplate.execute((ConnectionCallback<MigrationReport>) connection -> {
            if (!tryAcquireLock(connection)) {
                throw new IllegalStateException(
                        "another EPUB S3 migration run already holds lock '" + MIGRATION_LOCK_NAME + "'");
            }
            try {
                return action.get();
            } finally {
                releaseLock(connection);
            }
        });
    }

    private boolean tryAcquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ACQUIRE_LOCK)) {
            statement.setString(1, MIGRATION_LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private void releaseLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(RELEASE_LOCK)) {
            statement.setString(1, MIGRATION_LOCK_NAME);
            statement.execute();
        } catch (SQLException e) {
            log.warn("EPUB S3 migration lock release failed: cause={}", e.getClass().getSimpleName());
        }
    }

    private long count(String storageType) {
        Long value = jdbcTemplate.queryForObject(COUNT_BY_STORAGE_TYPE, Long.class, storageType);
        return value == null ? 0 : value;
    }

    private boolean migrateRow(EpubRow row) {
        BlobDigest digest;
        try {
            digest = readDigest(row.bookEpubId());
        } catch (RuntimeException e) {
            log.warn("EPUB S3 migration blob read failed: bookEpubId={}, cause={}",
                    row.bookEpubId(), e.getClass().getSimpleName());
            return false;
        }
        if (digest == null) {
            log.warn("EPUB S3 migration skipped, blob is missing: bookEpubId={}", row.bookEpubId());
            return false;
        }
        // 기존 DB metadata를 신뢰하기 전에 실제 blob과 대조한다.
        if (digest.size() != row.fileSize() || !digest.sha256().equalsIgnoreCase(nullToEmpty(row.sha256()))) {
            log.warn("EPUB S3 migration skipped, blob does not match metadata: bookEpubId={}", row.bookEpubId());
            return false;
        }

        StoredEpubObject stored;
        try {
            stored = upload(row, digest.size());
        } catch (RuntimeException e) {
            // upload 실패 시 DB row를 변경하지 않는다.
            log.warn("EPUB S3 migration upload failed: bookEpubId={}, cause={}",
                    row.bookEpubId(), e.getClass().getSimpleName());
            return false;
        }
        if (stored == null) {
            log.warn("EPUB S3 migration upload produced no object: bookEpubId={}", row.bookEpubId());
            return false;
        }

        try {
            int updated = jdbcTemplate.update(UPDATE_TO_S3, stored.key(), stored.versionId(), row.bookEpubId());
            if (updated != 1) {
                compensate(stored, row.bookEpubId(), null);
                log.warn("EPUB S3 migration metadata update matched {} rows: bookEpubId={}",
                        updated, row.bookEpubId());
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            compensate(stored, row.bookEpubId(), e);
            log.warn("EPUB S3 migration metadata update failed: bookEpubId={}, cause={}",
                    row.bookEpubId(), e.getClass().getSimpleName());
            return false;
        }
    }

    /** 보상 삭제. 실패해도 migration 전체를 중단시키지 않고 orphan으로 기록한다. */
    private void compensate(StoredEpubObject stored, long bookEpubId, RuntimeException original) {
        try {
            objectStorage.delete(stored.key(), stored.versionId());
        } catch (RuntimeException deleteFailure) {
            if (original != null) {
                original.addSuppressed(deleteFailure);
            }
            log.error("EPUB S3 migration compensating delete failed, object is orphaned: bookEpubId={}, cause={}",
                    bookEpubId, deleteFailure.getClass().getSimpleName());
        }
    }

    private BlobDigest readDigest(long bookEpubId) {
        return jdbcTemplate.query(SELECT_BLOB, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }
            try (InputStream input = resultSet.getBinaryStream(1)) {
                if (input == null) {
                    return null;
                }
                MessageDigest sha256 = newSha256();
                byte[] buffer = new byte[8192];
                long size = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    sha256.update(buffer, 0, read);
                    size += read;
                }
                return new BlobDigest(size, HexFormat.of().formatHex(sha256.digest()));
            } catch (IOException e) {
                throw new IllegalStateException("EPUB blob read failed: bookEpubId=" + bookEpubId, e);
            }
        }, bookEpubId);
    }

    private StoredEpubObject upload(EpubRow row, long contentLength) {
        return jdbcTemplate.query(SELECT_BLOB, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }
            try (InputStream input = resultSet.getBinaryStream(1)) {
                if (input == null) {
                    return null;
                }
                return objectStorage.put(row.bookId(), row.epubVersion(), row.sha256(), contentLength, input);
            } catch (IOException e) {
                throw new IllegalStateException("EPUB blob read failed: bookEpubId=" + row.bookEpubId(), e);
            }
        }, row.bookEpubId());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record EpubRow(long bookEpubId, long bookId, int epubVersion, long fileSize, String sha256) {
    }

    private record BlobDigest(long size, String sha256) {
    }
}
