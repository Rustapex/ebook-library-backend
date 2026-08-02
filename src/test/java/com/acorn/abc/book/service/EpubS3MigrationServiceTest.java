package com.acorn.abc.book.service;

import com.acorn.abc.book.service.EpubObjectStorage.StoredEpubObject;
import com.acorn.abc.book.service.EpubS3MigrationService.MigrationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EpubS3MigrationServiceTest {

    private static final long BOOK_EPUB_ID = 11L;
    private static final long BOOK_ID = 7L;
    private static final int EPUB_VERSION = 3;
    private static final byte[] CONTENT = "LEGACY-EPUB-BLOB".getBytes(StandardCharsets.UTF_8);
    private static final String SHA256 = sha256Hex(CONTENT);
    private static final String KEY = "epubs/7/3/" + SHA256 + ".epub";

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EpubObjectStorage objectStorage;

    @BeforeEach
    void setUp() throws Exception {
        givenSingleRunLockAcquired();
        given(objectStorage.isVersioningEnabled()).willReturn(true);
    }

    private EpubS3MigrationService service() {
        return new EpubS3MigrationService(jdbcTemplate, objectStorage);
    }

    // ---------- preflight: 단일 실행 guard ----------

    @Test
    void 다른_runner가_lock을_잡고있으면_아무것도하지않고_실패한다() throws Exception {
        givenSingleRunLockRejected();
        givenCounts(5L, 0L);
        givenNoInvariantViolations();

        assertThatThrownBy(() -> service().migrate(false, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EpubS3MigrationService.MIGRATION_LOCK_NAME);

        verifyNoInteractions(objectStorage);
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    // ---------- preflight: row invariant ----------

    @Test
    void dryRun에서도_손상row가있으면_failed로세고_아무것도옮기지않는다() {
        givenCounts(5L, 2L);
        // unknown type 1 + DB blob NULL 2 + DB에 key 존재 1 + S3 key blank 3 + S3 versionId blank 2
        givenInvariantViolations(1, 2, 1, 3, 2);

        MigrationReport report = service().migrate(true, 50);

        assertThat(report.invariantViolations()).isEqualTo(9);
        assertThat(report.failed()).isEqualTo(9);
        assertThat(report.migrated()).isZero();
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
        verify(objectStorage, never()).delete(anyString(), any());
    }

    @Test
    void 손상row가있으면_실제실행에서도_한건도옮기지않는다() {
        givenCounts(5L, 0L);
        givenInvariantViolations(0, 1, 0, 0, 0);

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.migrated()).isZero();
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), anyLong(), anyInt());
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void S3인데_storageKey가_blank인_row는_invariant위반으로_센다() {
        givenCounts(0L, 3L);
        givenInvariantViolations(0, 0, 0, 2, 0);

        MigrationReport report = service().migrate(true, 50);

        assertThat(report.invariantViolations()).isEqualTo(2);
        assertThat(report.failed()).isEqualTo(2);
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void S3인데_s3VersionId가_blank인_row는_invariant위반으로_센다() {
        givenCounts(0L, 3L);
        givenInvariantViolations(0, 0, 0, 0, 3);

        MigrationReport report = service().migrate(true, 50);

        assertThat(report.invariantViolations()).isEqualTo(3);
        assertThat(report.failed()).isEqualTo(3);
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void 손상row가있으면_runner_exitCode가_dryRun에서도_non_zero다() {
        EpubS3MigrationService migrationService = mock(EpubS3MigrationService.class);
        given(migrationService.migrate(true, 50)).willReturn(new MigrationReport(5, 0, 2, 7, 7));

        assertThat(EpubS3MigrationRunner.run(runnerContext(migrationService, "true", "true"))).isEqualTo(1);
    }

    // ---------- preflight: bucket versioning ----------

    @Test
    void bucket_versioning이_꺼져있으면_migration을_시작하지않는다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        given(objectStorage.isVersioningEnabled()).willReturn(false);

        assertThatThrownBy(() -> service().migrate(false, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("versioning");

        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void dryRun도_versioning_preflight를_통과해야한다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        given(objectStorage.isVersioningEnabled()).willReturn(false);

        assertThatThrownBy(() -> service().migrate(true, 50))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- 정상 동작 ----------

    @Test
    void dryRun은_DB와_S3를_전혀_변경하지않고_대상건수만_보고한다() {
        givenCounts(5L, 2L);
        givenNoInvariantViolations();

        MigrationReport report = service().migrate(true, 50);

        assertThat(report.candidates()).isEqualTo(5);
        assertThat(report.migrated()).isZero();
        assertThat(report.skipped()).isEqualTo(2);
        assertThat(report.failed()).isZero();
        assertThat(report.invariantViolations()).isZero();

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), anyLong(), anyInt());
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
        verify(objectStorage, never()).delete(anyString(), any());
    }

    @Test
    void DB_row를_S3로_옮기고_metadata를_갱신한다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        givenSingleRowBatch();
        givenBlobStream();
        given(objectStorage.put(eq(BOOK_ID), eq(EPUB_VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        given(jdbcTemplate.update(anyString(), eq(KEY), eq("ver-1"), eq(BOOK_EPUB_ID))).willReturn(1);

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.migrated()).isEqualTo(1);
        assertThat(report.failed()).isZero();
        // rollback용 epub_data는 유지한다: UPDATE는 storage metadata만 건드린다.
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("SET storage_type = 'S3'"),
                eq(KEY), eq("ver-1"), eq(BOOK_EPUB_ID));
        verify(objectStorage, never()).delete(anyString(), any());
    }

    @Test
    void 이미_S3인_row만_있으면_재실행시_skip한다() {
        givenCounts(0L, 4L);
        givenNoInvariantViolations();
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong(), anyInt()))
                .willReturn(List.of());

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.candidates()).isZero();
        assertThat(report.migrated()).isZero();
        assertThat(report.skipped()).isEqualTo(4);
        assertThat(report.failed()).isZero();
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    @Test
    void upload가실패하면_DB_row를_변경하지않는다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        givenSingleRowBatch();
        givenBlobStream();
        willThrow(new IllegalStateException("s3 unavailable"))
                .given(objectStorage).put(anyLong(), anyInt(), anyString(), anyLong(), any());

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.migrated()).isZero();
        assertThat(report.failed()).isEqualTo(1);
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
        verify(objectStorage, never()).delete(anyString(), any());
    }

    @Test
    void DB_update가실패하면_업로드한_exact_version을_보상삭제한다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        givenSingleRowBatch();
        givenBlobStream();
        given(objectStorage.put(eq(BOOK_ID), eq(EPUB_VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        willThrow(new DataAccessResourceFailureException("db down"))
                .given(jdbcTemplate).update(anyString(), eq(KEY), eq("ver-1"), eq(BOOK_EPUB_ID));

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.migrated()).isZero();
        assertThat(report.failed()).isEqualTo(1);
        verify(objectStorage).delete(KEY, "ver-1");
    }

    @Test
    void blob이_metadata와_다르면_upload하지않고_failed로_센다() {
        givenCounts(1L, 0L);
        givenNoInvariantViolations();
        // file_size가 실제 blob 크기와 다른 row
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(0L), eq(50)))
                .willAnswer(invocation -> List.of(mapRow(invocation.getArgument(1), CONTENT.length + 1, SHA256)));
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_EPUB_ID), eq(50)))
                .willReturn(List.of());
        givenBlobStream();

        MigrationReport report = service().migrate(false, 50);

        assertThat(report.migrated()).isZero();
        assertThat(report.failed()).isEqualTo(1);
        verify(objectStorage, never()).put(anyLong(), anyInt(), anyString(), anyLong(), any());
    }

    // ---------- runner exit code ----------

    @Test
    void failed가_1건이상이면_runner_exitCode가_non_zero다() {
        EpubS3MigrationService migrationService = mock(EpubS3MigrationService.class);
        given(migrationService.migrate(false, 50)).willReturn(new MigrationReport(1, 0, 0, 1, 0));

        assertThat(EpubS3MigrationRunner.run(runnerContext(migrationService, "true", "false"))).isEqualTo(1);
    }

    @Test
    void failed가_0건이면_runner_exitCode가_0이다() {
        EpubS3MigrationService migrationService = mock(EpubS3MigrationService.class);
        given(migrationService.migrate(false, 50)).willReturn(new MigrationReport(3, 3, 0, 0, 0));

        assertThat(EpubS3MigrationRunner.run(runnerContext(migrationService, "true", "false"))).isZero();
    }

    @Test
    void runner는_optin이없으면_migration을_실행하지않는다() {
        EpubS3MigrationService migrationService = mock(EpubS3MigrationService.class);

        assertThat(EpubS3MigrationRunner.run(runnerContext(migrationService, "false", "false"))).isEqualTo(2);

        verify(migrationService, never()).migrate(org.mockito.ArgumentMatchers.anyBoolean(), anyInt());
    }

    @Test
    void runner는_preflight예외를_exitCode_1로_변환한다() {
        EpubS3MigrationService migrationService = mock(EpubS3MigrationService.class);
        given(migrationService.migrate(false, 50))
                .willThrow(new IllegalStateException("another EPUB S3 migration run already holds lock"));

        assertThat(EpubS3MigrationRunner.run(runnerContext(migrationService, "true", "false"))).isEqualTo(1);
    }

    // ---------- helpers ----------

    private ConfigurableApplicationContext runnerContext(EpubS3MigrationService migrationService,
                                                         String enabled, String dryRun) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.epub-migration.enabled", enabled);
        environment.setProperty("app.epub-migration.dry-run", dryRun);
        environment.setProperty("app.epub-migration.batch-size", "50");
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        given(context.getEnvironment()).willReturn(environment);
        given(context.getBean(EpubS3MigrationService.class)).willReturn(migrationService);
        return context;
    }

    /** GET_LOCK이 1을 반환하는 전용 connection을 흉내낸다. */
    private void givenSingleRunLockAcquired() throws Exception {
        givenSingleRunLock(1);
    }

    /** GET_LOCK이 0을 반환해 lock 획득에 실패하는 상황. */
    private void givenSingleRunLockRejected() throws Exception {
        givenSingleRunLock(0);
    }

    private void givenSingleRunLock(int lockResultValue) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement acquire = mock(PreparedStatement.class);
        PreparedStatement release = mock(PreparedStatement.class);
        ResultSet lockResult = mock(ResultSet.class);
        given(connection.prepareStatement("SELECT GET_LOCK(?, 0)")).willReturn(acquire);
        given(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).willReturn(release);
        given(acquire.executeQuery()).willReturn(lockResult);
        given(lockResult.next()).willReturn(true);
        given(lockResult.getInt(1)).willReturn(lockResultValue);
        given(jdbcTemplate.execute(any(ConnectionCallback.class)))
                .willAnswer(invocation -> ((ConnectionCallback<?>) invocation.getArgument(0))
                        .doInConnection(connection));
    }

    private void givenCounts(long dbRows, long s3Rows) {
        given(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("DB"))).willReturn(dbRows);
        given(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("S3"))).willReturn(s3Rows);
    }

    private void givenNoInvariantViolations() {
        givenInvariantViolations(0, 0, 0, 0, 0);
    }

    private void givenInvariantViolations(long unknownType, long dbMissingBlob, long dbWithKey,
                                          long s3MissingKey, long s3MissingVersionId) {
        given(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .willAnswer(invocation -> {
                    RowMapper<?> rowMapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    given(resultSet.getLong(1)).willReturn(unknownType);
                    given(resultSet.getLong(2)).willReturn(dbMissingBlob);
                    given(resultSet.getLong(3)).willReturn(dbWithKey);
                    given(resultSet.getLong(4)).willReturn(s3MissingKey);
                    given(resultSet.getLong(5)).willReturn(s3MissingVersionId);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });
    }

    private void givenSingleRowBatch() {
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(0L), eq(50)))
                .willAnswer(invocation -> List.of(mapRow(invocation.getArgument(1), CONTENT.length, SHA256)));
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_EPUB_ID), eq(50)))
                .willReturn(List.of());
    }

    private Object mapRow(RowMapper<?> rowMapper, long fileSize, String sha256) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getLong(1)).willReturn(BOOK_EPUB_ID);
        given(resultSet.getLong(2)).willReturn(BOOK_ID);
        given(resultSet.getInt(3)).willReturn(EPUB_VERSION);
        given(resultSet.getLong(4)).willReturn(fileSize);
        given(resultSet.getString(5)).willReturn(sha256);
        return rowMapper.mapRow(resultSet, 0);
    }

    private void givenBlobStream() {
        given(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(BOOK_EPUB_ID)))
                .willAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    given(resultSet.next()).willReturn(true);
                    given(resultSet.getBinaryStream(1)).willReturn(new ByteArrayInputStream(CONTENT));
                    return extractor.extractData(resultSet);
                });
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
