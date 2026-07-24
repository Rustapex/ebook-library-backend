-- EPUB S3 object storage 전환용 additive migration.
-- 관련 계획: .omc/plans/claude-deployment-code-implementation.md
--
-- 적용 대상: book_epub이 이미 존재하는 기존 DB(V001 적용 완료 상태).
-- 신규 빈 DB는 src/main/resources/schema.sql이 최종 구조를 이미 포함하므로 이 파일을 실행하지 않는다.
--
-- 성질:
--   - additive + idempotent. 2회 이상 실행해도 안전하다.
--   - 기존 epub_data를 삭제하거나 NULL로 만들지 않는다.
--   - 기존 row는 storage_type='DB'로 남는다.
--   - 자동 rollback SQL을 제공하지 않는다(U003 없음). 되돌리려면 사전 backup/dump를 복원한다.
--
-- 실패 정책:
--   구조가 기대와 조금이라도 다르면 SIGNAL SQLSTATE '45000'으로 즉시 중단하고 mysql client가
--   non-zero exit code를 반환한다. 부분 적용(3개 중 1~2개 column만 존재) 상태도 실패로 처리한다.
--   preflight와 postflight를 모두 검사하므로, 기대 구조가 아니면 exit 0이 나오지 않는다.
--
-- 실행 방법 (DELIMITER는 mysql client 명령이므로 반드시 mysql CLI로 실행한다):
--   mysql -h <host> -u <user> -p <schema> < V003__epub_s3_object_storage.sql
--   echo "exit code: $?"   # 0이어야 성공
--
-- 실행 전 조건:
--   - docs/데이터베이스/README.md의 read-only 상태 판별을 먼저 수행한다.
--   - MySQL DDL은 implicit commit되므로 실행 전 새 backup과 복원 가능성을 확인한다.
--   - 이 파일의 실제 DB 적용은 코드 구현 범위 밖이며 사용자가 별도로 수행한다.

SELECT DATABASE() AS current_database, VERSION() AS mysql_version;

DROP PROCEDURE IF EXISTS abc_v003_epub_s3_object_storage;

DELIMITER $$

CREATE PROCEDURE abc_v003_epub_s3_object_storage()
BEGIN
  DECLARE v_table_count      INT DEFAULT 0;
  DECLARE v_base_ok          INT DEFAULT 0;
  DECLARE v_new_cols         INT DEFAULT 0;
  DECLARE v_new_ok           INT DEFAULT 0;
  DECLARE v_uk_version_ok    INT DEFAULT 0;
  DECLARE v_uk_key_exists    INT DEFAULT 0;
  DECLARE v_uk_key_ok        INT DEFAULT 0;
  DECLARE v_epub_data_ok     INT DEFAULT 0;

  -- ============================================================
  -- 1. Preflight: book_epub table 존재
  -- ============================================================
  SELECT COUNT(*) INTO v_table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'book_epub';

  IF v_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 abort: book_epub table not found. Apply V001 first.';
  END IF;

  -- ============================================================
  -- 2. Preflight: 기존 column의 정확한 type / nullability / default
  --    9개가 모두 정확히 일치해야 한다. 하나라도 다르면 MIXED/UNKNOWN으로 보고 중단한다.
  -- ============================================================
  SELECT COUNT(*) INTO v_base_ok
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'book_epub'
    AND (
         (column_name = 'book_epub_id'       AND column_type = 'bigint unsigned' AND is_nullable = 'NO'
                                             AND extra LIKE '%auto_increment%')
      OR (column_name = 'book_id'            AND column_type = 'bigint unsigned' AND is_nullable = 'NO')
      OR (column_name = 'original_file_name' AND column_type = 'varchar(255)'    AND is_nullable = 'NO')
      OR (column_name = 'content_type'       AND column_type = 'varchar(100)'    AND is_nullable = 'NO')
      OR (column_name = 'file_size'          AND column_type = 'bigint unsigned' AND is_nullable = 'NO')
      OR (column_name = 'sha256'             AND column_type = 'char(64)'        AND is_nullable = 'NO')
      OR (column_name = 'epub_version'       AND column_type = 'int unsigned'    AND is_nullable = 'NO'
                                             AND column_default = '1')
      OR (column_name = 'epub_data'          AND column_type = 'longblob')
      OR (column_name = 'created_at'         AND column_type = 'datetime'        AND is_nullable = 'NO')
    );

  IF v_base_ok <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 abort: book_epub base column type/nullability/default mismatch.';
  END IF;

  -- ============================================================
  -- 3. Preflight: 신규 column은 0개(미적용) 또는 3개(적용 완료)만 허용
  --    1~2개면 부분 적용된 손상 상태다.
  -- ============================================================
  SELECT COUNT(*) INTO v_new_cols
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'book_epub'
    AND column_name IN ('storage_type', 'storage_key', 's3_version_id');

  IF v_new_cols <> 0 AND v_new_cols <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 abort: partial S3 columns present. Expected 0 or 3. MIXED state.';
  END IF;

  -- ============================================================
  -- 4. Preflight: 이미 적용돼 있다면 정확한 구조인지 검증
  -- ============================================================
  IF v_new_cols = 3 THEN
    SELECT COUNT(*) INTO v_new_ok
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'book_epub'
      AND (
           (column_name = 'storage_type'  AND column_type = 'varchar(16)'  AND is_nullable = 'NO'
                                          AND column_default = 'DB')
        OR (column_name = 'storage_key'   AND column_type = 'varchar(512)' AND is_nullable = 'YES')
        OR (column_name = 's3_version_id' AND column_type = 'varchar(128)' AND is_nullable = 'YES')
      );

    IF v_new_ok <> 3 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V003 abort: existing S3 columns have wrong type/nullability/default.';
    END IF;
  END IF;

  -- ============================================================
  -- 5. Preflight: uk_book_epub_book_version이 UNIQUE(book_id, epub_version)인지
  -- ============================================================
  SELECT COUNT(*) INTO v_uk_version_ok
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'book_epub'
      AND index_name = 'uk_book_epub_book_version' AND non_unique = 0
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'book_id,epub_version'
  ) t;

  IF v_uk_version_ok <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 abort: uk_book_epub_book_version must be UNIQUE(book_id,epub_version).';
  END IF;

  -- ============================================================
  -- 6. Preflight: uk_book_epub_storage_key가 있다면 UNIQUE(storage_key) 단일 column이어야 한다
  -- ============================================================
  SELECT COUNT(DISTINCT index_name) INTO v_uk_key_exists
  FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'book_epub'
    AND index_name = 'uk_book_epub_storage_key';

  IF v_uk_key_exists > 0 THEN
    SELECT COUNT(*) INTO v_uk_key_ok
    FROM (
      SELECT index_name
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'book_epub'
        AND index_name = 'uk_book_epub_storage_key' AND non_unique = 0
      GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'storage_key'
    ) t;

    IF v_uk_key_ok <> 1 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V003 abort: uk_book_epub_storage_key is not UNIQUE(storage_key).';
    END IF;
  END IF;

  -- ============================================================
  -- 7. Preflight (data): 모든 DDL 이전에 수행한다.
  --    여기서 실패하면 ALTER TABLE을 단 한 번도 실행하지 않는다.
  -- ============================================================
  SET @v003_bad := 0;
  SET @v003_dup := 0;

  IF v_new_cols = 0 THEN
    -- 신규 column이 없으면 migration 후 모든 row가 storage_type='DB'가 된다.
    -- epub_data가 NULL인 row가 있으면 그 순간 DB invariant(epub_data NOT NULL)를 위반한다.
    -- column을 추가하기 전에 막아야 손상 상태가 고착되지 않는다.
    SET @v003_sql := 'SELECT COUNT(*) INTO @v003_bad FROM book_epub WHERE epub_data IS NULL';
    PREPARE v003_pre_stmt FROM @v003_sql;
    EXECUTE v003_pre_stmt;
    DEALLOCATE PREPARE v003_pre_stmt;

    IF @v003_bad > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V003 abort: epub_data IS NULL rows exist before adding S3 columns.';
    END IF;
  ELSE
    -- 이미 신규 column이 있는 상태: 전체 row invariant를 DDL 전에 검사한다.
    SET @v003_sql := CONCAT(
        'SELECT COUNT(*) INTO @v003_bad FROM book_epub WHERE ',
        '(storage_type IS NULL OR storage_type NOT IN (''DB'', ''S3'')) ',
        'OR (storage_type = ''DB'' AND epub_data IS NULL) ',
        'OR (storage_type = ''DB'' AND storage_key IS NOT NULL) ',
        'OR (storage_type = ''S3'' AND (storage_key IS NULL OR TRIM(storage_key) = '''')) ',
        'OR (storage_type = ''S3'' AND (s3_version_id IS NULL OR TRIM(s3_version_id) = ''''))');
    PREPARE v003_pre_stmt FROM @v003_sql;
    EXECUTE v003_pre_stmt;
    DEALLOCATE PREPARE v003_pre_stmt;

    IF @v003_bad > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V003 abort: existing rows violate storage invariants (pre-DDL).';
    END IF;

    -- storage_key에 중복이 있으면 UNIQUE index 생성이 실패한다.
    -- ALTER를 시도했다가 중간에 깨지는 대신 미리 막는다.
    SET @v003_sql := CONCAT(
        'SELECT COUNT(*) INTO @v003_dup FROM (',
        'SELECT storage_key FROM book_epub WHERE storage_key IS NOT NULL ',
        'GROUP BY storage_key HAVING COUNT(*) > 1) dup_keys');
    PREPARE v003_pre_stmt FROM @v003_sql;
    EXECUTE v003_pre_stmt;
    DEALLOCATE PREPARE v003_pre_stmt;

    IF @v003_dup > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V003 abort: duplicate storage_key values block UNIQUE index creation.';
    END IF;
  END IF;

  -- ============================================================
  -- 8. DDL 적용 (모두 조건부). 위 preflight를 모두 통과한 뒤에만 도달한다.
  -- ============================================================

  -- 8-1. epub_data를 nullable로 변경 (이미 nullable이면 skip)
  SELECT COUNT(*) INTO v_epub_data_ok
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'book_epub'
    AND column_name = 'epub_data' AND is_nullable = 'YES';

  IF v_epub_data_ok = 0 THEN
    ALTER TABLE book_epub MODIFY COLUMN epub_data LONGBLOB NULL;
  END IF;

  -- 8-2. 신규 column 3개 추가 (0개일 때만 한 번에 추가한다. 부분 적용은 3단계에서 이미 차단했다)
  IF v_new_cols = 0 THEN
    ALTER TABLE book_epub
      ADD COLUMN storage_type  VARCHAR(16)  NOT NULL DEFAULT 'DB' AFTER epub_data,
      ADD COLUMN storage_key   VARCHAR(512) NULL                  AFTER storage_type,
      ADD COLUMN s3_version_id VARCHAR(128) NULL                  AFTER storage_key;
  END IF;

  -- 8-3. storage_key UNIQUE 추가 (없을 때만)
  --      MySQL UNIQUE index는 NULL 중복을 허용하므로 DB row 다건과 공존한다.
  IF v_uk_key_exists = 0 THEN
    ALTER TABLE book_epub ADD CONSTRAINT uk_book_epub_storage_key UNIQUE (storage_key);
  END IF;

  -- 8-4. 기존 row 보정: storage_type이 비어 있는 row만 'DB'로 채운다. epub_data는 건드리지 않는다.
  -- CONCAT을 쓴다. MySQL 기본 sql_mode에서 ||는 문자열 연결이 아니라 논리 OR다.
  SET @v003_fix := CONCAT(
      'UPDATE book_epub SET storage_type = ''DB'' ',
      'WHERE storage_type IS NULL OR storage_type = ''''');
  PREPARE v003_fix_stmt FROM @v003_fix;
  EXECUTE v003_fix_stmt;
  DEALLOCATE PREPARE v003_fix_stmt;

  -- ============================================================
  -- 9. Postflight: 최종 구조 재검증. 기대 구조가 아니면 여기서 실패한다.
  -- ============================================================
  SELECT COUNT(*) INTO v_new_ok
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'book_epub'
    AND (
         (column_name = 'epub_data'     AND column_type = 'longblob'     AND is_nullable = 'YES')
      OR (column_name = 'storage_type'  AND column_type = 'varchar(16)'  AND is_nullable = 'NO'
                                        AND column_default = 'DB')
      OR (column_name = 'storage_key'   AND column_type = 'varchar(512)' AND is_nullable = 'YES')
      OR (column_name = 's3_version_id' AND column_type = 'varchar(128)' AND is_nullable = 'YES')
    );

  IF v_new_ok <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 postflight failed: final book_epub column contract mismatch.';
  END IF;

  SELECT COUNT(*) INTO v_uk_key_ok
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'book_epub'
      AND index_name = 'uk_book_epub_storage_key' AND non_unique = 0
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'storage_key'
  ) t;

  IF v_uk_key_ok <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 postflight failed: uk_book_epub_storage_key missing or wrong columns.';
  END IF;

  SELECT COUNT(*) INTO v_uk_version_ok
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'book_epub'
      AND index_name = 'uk_book_epub_book_version' AND non_unique = 0
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'book_id,epub_version'
  ) t;

  IF v_uk_version_ok <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 postflight failed: uk_book_epub_book_version was lost.';
  END IF;

  -- ============================================================
  -- 10. Postflight: 저장소 data invariant
  --      DB row: epub_data NOT NULL, storage_key NULL
  --      S3 row: storage_key와 s3_version_id 모두 non-blank (공백 문자열도 위반)
  --      unknown storage_type 금지
  --    application(BookEpubBinaryStore, EpubS3MigrationService)과 같은 계약이다.
  -- ============================================================
  SET @v003_bad := 0;
  SET @v003_check := CONCAT(
      'SELECT COUNT(*) INTO @v003_bad FROM book_epub WHERE ',
      '(storage_type IS NULL OR storage_type NOT IN (''DB'', ''S3'')) ',
      'OR (storage_type = ''DB'' AND epub_data IS NULL) ',
      'OR (storage_type = ''DB'' AND storage_key IS NOT NULL) ',
      'OR (storage_type = ''S3'' AND (storage_key IS NULL OR TRIM(storage_key) = '''')) ',
      'OR (storage_type = ''S3'' AND (s3_version_id IS NULL OR TRIM(s3_version_id) = ''''))');
  PREPARE v003_check_stmt FROM @v003_check;
  EXECUTE v003_check_stmt;
  DEALLOCATE PREPARE v003_check_stmt;

  IF @v003_bad > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V003 postflight failed: book_epub rows violate storage invariants.';
  END IF;
END$$

DELIMITER ;

CALL abc_v003_epub_s3_object_storage();

DROP PROCEDURE abc_v003_epub_s3_object_storage;

-- ============================================================
-- 11. 참고용 최종 상태 출력 (검증은 위 procedure가 이미 강제했다)
-- ============================================================
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'book_epub'
  AND column_name IN ('epub_data', 'storage_type', 'storage_key', 's3_version_id')
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'book_epub'
  AND index_name IN ('uk_book_epub_book_version', 'uk_book_epub_storage_key')
GROUP BY INDEX_NAME, NON_UNIQUE
ORDER BY INDEX_NAME;

SELECT storage_type, COUNT(*) AS row_count,
       SUM(epub_data IS NULL) AS null_blob_count,
       SUM(storage_key IS NOT NULL) AS storage_key_count
FROM book_epub
GROUP BY storage_type;
