package com.acorn.abc.book.service;

import com.acorn.abc.book.service.EpubObjectStorage.StoredEpubObject;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookEpubBinaryStoreTest {

    private static final Long BOOK_ID = 7L;
    private static final int VERSION = 3;
    private static final String SHA256 = "a".repeat(64);
    private static final String KEY = "epubs/7/3/" + SHA256 + ".epub";
    private static final byte[] CONTENT = "EPUB-CONTENT".getBytes(StandardCharsets.UTF_8);

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EpubObjectStorage objectStorage;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------- DB mode ----------

    @Test
    void dbMode_insert는_기존대로_LONGBLOB에_저장한다() throws Exception {
        BookEpubBinaryStore store = dbModeStore();
        givenBookLockAndNextVersion();
        given(jdbcTemplate.update(any(PreparedStatementCreator.class))).willReturn(1);

        int version = store.insertNextVersion(BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile());

        assertThat(version).isEqualTo(VERSION);
        verify(jdbcTemplate).update(any(PreparedStatementCreator.class));
        verifyNoInteractions(objectStorage);
    }

    @Test
    void dbMode_read는_기존대로_LONGBLOB을_streaming한다() {
        BookEpubBinaryStore store = dbModeStore();
        givenLocationRow("DB", null, null);
        givenBlobStream(CONTENT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        store.writeTo(BOOK_ID, VERSION, output);

        assertThat(output.toByteArray()).isEqualTo(CONTENT);
        verifyNoInteractions(objectStorage);
    }

    @Test
    void read_row가없으면_BOOK_EPUB_NOT_FOUND이다() {
        BookEpubBinaryStore store = dbModeStore();
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_ID), eq(VERSION)))
                .willReturn(List.of());

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_EPUB_NOT_FOUND);
    }

    // ---------- S3 mode ----------

    @Test
    void s3Mode_insert는_metadata만_저장하고_epubData는_NULL이다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        given(jdbcTemplate.update(any(PreparedStatementCreator.class))).willReturn(1);

        int version = store.insertNextVersion(BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile());

        assertThat(version).isEqualTo(VERSION);
        // storage_type='S3', epub_data=NULL 인 INSERT SQL 하나만 실행한다.
        verify(jdbcTemplate).update(any(PreparedStatementCreator.class));
        verify(objectStorage, never()).delete(anyString(), anyString());
    }

    @Test
    void s3Mode_DB_insert가실패하면_업로드한_S3_object를_삭제한다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(jdbcTemplate).update(any(PreparedStatementCreator.class));

        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(objectStorage).delete(KEY, "ver-1");
    }

    @Test
    void s3Mode_transaction이_rollback되면_S3_object를_보상삭제한다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        given(jdbcTemplate.update(any(PreparedStatementCreator.class))).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        store.insertNextVersion(BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile());
        verify(objectStorage, never()).delete(anyString(), anyString());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(objectStorage).delete(KEY, "ver-1");
    }

    @Test
    void s3Mode_transaction이_commit되면_S3_object를_삭제하지않는다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        given(jdbcTemplate.update(any(PreparedStatementCreator.class))).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        store.insertNextVersion(BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile());
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }

        verify(objectStorage, never()).delete(anyString(), anyString());
    }

    @Test
    void s3Mode_transaction결과가_UNKNOWN이면_삭제하지않고_reconciliation대상으로남긴다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        given(jdbcTemplate.update(any(PreparedStatementCreator.class))).willReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        store.insertNextVersion(BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile());
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
        }

        // commit 여부를 모르므로 지우면 안 된다. 지웠다가 실제로 commit됐다면 데이터가 사라진다.
        verify(objectStorage, never()).delete(anyString(), any());
    }

    @Test
    void s3Mode_보상삭제실패는_원래DB예외를가리지않고_suppressed로기록된다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        DataIntegrityViolationException dbFailure = new DataIntegrityViolationException("duplicate");
        willThrow(dbFailure).given(jdbcTemplate).update(any(PreparedStatementCreator.class));
        willThrow(new IllegalStateException("s3 delete failed")).given(objectStorage).delete(KEY, "ver-1");

        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isSameAs(dbFailure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed())
                        .singleElement()
                        .isInstanceOf(IllegalStateException.class));
    }

    @Test
    void s3Mode_보상삭제가실패하면_완료처리하지않고_rollback에서재시도한다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(jdbcTemplate).update(any(PreparedStatementCreator.class));
        // 첫 삭제는 실패, rollback 시점의 재시도는 성공
        willThrow(new IllegalStateException("s3 delete failed")).willDoNothing()
                .given(objectStorage).delete(KEY, "ver-1");

        TransactionSynchronizationManager.initSynchronization();
        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isInstanceOf(DataIntegrityViolationException.class);

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(objectStorage, times(2)).delete(KEY, "ver-1");
    }

    @Test
    void s3Mode_보상삭제가성공하면_rollback에서다시삭제하지않는다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        given(objectStorage.put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any()))
                .willReturn(new StoredEpubObject(KEY, "ver-1"));
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(jdbcTemplate).update(any(PreparedStatementCreator.class));

        TransactionSynchronizationManager.initSynchronization();
        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isInstanceOf(DataIntegrityViolationException.class);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(objectStorage, times(1)).delete(KEY, "ver-1");
    }

    @Test
    void 알수없는storageType은_DB로_fallback하지않고_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("GCS", "some/key", null);

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage_type");

        verify(objectStorage, never()).writeTo(anyString(), any(), any());
        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION));
    }

    @Test
    void storageType이_비어있으면_DB로_fallback하지않고_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow(null, null, null);

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class);

        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION));
    }

    @Test
    void s3Mode_read는_key와_versionId로_S3에서_streaming한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", KEY, "ver-1");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        store.writeTo(BOOK_ID, VERSION, output);

        verify(objectStorage).writeTo(eq(KEY), eq("ver-1"), eq(output));
        // S3 row는 DB blob을 조회하지 않는다.
        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION));
    }

    @Test
    void s3Mode_기존DB_row는_계속_LONGBLOB에서_읽는다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("DB", null, null);
        givenBlobStream(CONTENT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        store.writeTo(BOOK_ID, VERSION, output);

        assertThat(output.toByteArray()).isEqualTo(CONTENT);
        verify(objectStorage, never()).writeTo(anyString(), any(), any());
    }

    @Test
    void s3Mode_metadata가깨진_S3_row는_DB로_fallback하지않고_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", null, null);

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class);

        verify(objectStorage, never()).writeTo(anyString(), any(), any());
        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION));
    }

    @Test
    void s3Mode_s3VersionId가_blank인_row는_최신version으로_fallback하지않고_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", KEY, "   ");

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("s3_version_id");

        verify(objectStorage, never()).writeTo(anyString(), any(), any());
    }

    @Test
    void s3Mode_s3VersionId가_NULL인_row도_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", KEY, null);

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("s3_version_id");

        verify(objectStorage, never()).writeTo(anyString(), any(), any());
    }

    @Test
    void s3Mode_storageKey가_공백문자열이면_실패한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", "   ", "ver-1");

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage_key");

        verify(objectStorage, never()).writeTo(anyString(), any(), any());
        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION));
    }

    @Test
    void s3Mode_put이실패하면_DB_metadata를_저장하지않는다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        givenBookLockAndNextVersion();
        // versionId를 확보하지 못한 put은 예외를 던진다(S3EpubObjectStorage 계약).
        willThrow(new IllegalStateException("blank versionId; bucket versioning must be enabled"))
                .given(objectStorage).put(eq(BOOK_ID), eq(VERSION), eq(SHA256), eq((long) CONTENT.length), any());

        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isInstanceOf(IllegalStateException.class);

        verify(jdbcTemplate, never()).update(any(PreparedStatementCreator.class));
    }

    @Test
    void s3Mode_S3장애는_BusinessException으로_감싸지않고_전역500으로_전파한다() {
        BookEpubBinaryStore store = s3ModeStore();
        givenLocationRow("S3", KEY, "ver-1");
        willThrow(new IllegalStateException("s3 unavailable"))
                .given(objectStorage).writeTo(anyString(), any(), any());

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(BusinessException.class);
    }

    @Test
    void s3Mode인데_objectStorage_bean이없으면_기동을실패시킨다() {
        assertThatThrownBy(() -> new BookEpubBinaryStore(jdbcTemplate, Optional.empty(), "s3"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- helpers ----------

    private BookEpubBinaryStore dbModeStore() {
        return new BookEpubBinaryStore(jdbcTemplate, Optional.of(objectStorage), "db");
    }

    private BookEpubBinaryStore s3ModeStore() {
        return new BookEpubBinaryStore(jdbcTemplate, Optional.of(objectStorage), "s3");
    }

    private MultipartFile multipartFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.getInputStream()).willReturn(new ByteArrayInputStream(CONTENT));
        return file;
    }

    private void givenBookLockAndNextVersion() {
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_ID))).willReturn(List.of(BOOK_ID));
        given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(BOOK_ID))).willReturn(VERSION);
    }

    private void givenLocationRow(String storageType, String storageKey, String versionId) {
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_ID), eq(VERSION)))
                .willAnswer(invocation -> {
                    RowMapper<?> rowMapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    given(resultSet.getString(1)).willReturn(storageType);
                    given(resultSet.getString(2)).willReturn(storageKey);
                    given(resultSet.getString(3)).willReturn(versionId);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });
    }

    private void givenBlobStream(byte[] content) {
        given(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION)))
                .willAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    given(resultSet.next()).willReturn(true);
                    given(resultSet.getBinaryStream(1)).willReturn(new ByteArrayInputStream(content));
                    return extractor.extractData(resultSet);
                });
    }

    @Test
    void dbMode_blob이_NULL이면_조용히성공하지않고_실패한다() {
        BookEpubBinaryStore store = dbModeStore();
        givenLocationRow("DB", null, null);
        given(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(BOOK_ID), eq(VERSION)))
                .willAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    given(resultSet.next()).willReturn(true);
                    given(resultSet.getBinaryStream(1)).willReturn(null);
                    return extractor.extractData(resultSet);
                });

        assertThatThrownBy(() -> store.writeTo(BOOK_ID, VERSION, new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void insert_도서가없으면_BOOK_NOT_FOUND이고_S3에_업로드하지않는다() throws Exception {
        BookEpubBinaryStore store = s3ModeStore();
        given(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(BOOK_ID))).willReturn(List.of());

        assertThatThrownBy(() -> store.insertNextVersion(
                BOOK_ID, "book.epub", CONTENT.length, SHA256, multipartFile()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);

        verify(objectStorage, times(0)).put(any(), org.mockito.ArgumentMatchers.anyInt(),
                anyString(), org.mockito.ArgumentMatchers.anyLong(), any());
    }
}
