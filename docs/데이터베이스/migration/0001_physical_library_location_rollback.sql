-- 0001_physical_library_location.sql 보상(rollback) SQL
-- MySQL DDL은 auto-commit이므로 이것은 transaction rollback이 아니라 별도 보상 SQL이다.
-- 실행 전 반드시 DB backup을 확보한다. print_isbn13/library_holding에 실제로 채워진
-- 운영 데이터가 있다면 이 스크립트로 되돌릴 수 없으므로 backup에서 복원해야 한다.
--
-- 이 스크립트는 "지금 존재하는가"가 아니라 _migration_0001_state 기록("이 migration이 실제로
-- 만들었는가")을 기준으로만 제거한다. migration 적용 이전부터 우연히 같은 이름으로 존재하던
-- 컬럼/index/테이블은 created_by_migration=0으로 기록되어 있어 건드리지 않는다.
--
-- _migration_0001_state 자체가 없으면(이 방식으로 migration을 적용한 적이 없거나 이미 rollback되어
-- 정리된 상태) 그 테이블을 직접 SELECT하면 MySQL 1146(테이블 없음) 오류로 스크립트 전체가 중단된다.
-- 이를 피하려고 "_migration_0001_state를 읽는 SQL 자체"도 @state_table_exists로 먼저 분기해
-- PREPARE/EXECUTE로만 실행한다(스토어드 프로시저/DELIMITER 없이 mysql, mysqlsh 등 어떤 배치
-- 클라이언트에서도 동일하게 동작하도록 SET 문 기반 동적 SQL만 사용한다).
--
-- 제거 순서: library_holding index -> library_holding table -> library 추가 index/컬럼
--           -> book 추가 index/컬럼 -> tracking table (FK 의존관계 역순)
--
-- [tracking table 삭제 조건]
-- 각 DROP 단계는 개별적으로 성공/무시될 수 있지만, 그것만으로 "이 migration이 만든 객체가
-- 전부 제거됐다"고 단정할 수 없다(예: crash-resume으로 created_by_migration이 0으로 잘못
-- 남아있던 객체, 또는 락/권한 문제로 DROP이 조용히 SELECT 1로 스킵된 경우). 그래서 모든 개별
-- 제거 시도가 끝난 뒤, _migration_0001_state에 created_by_migration=1로 기록된 각 객체를
-- INFORMATION_SCHEMA로 다시 조회해 "실제로 지금 남아있는지" 최종 검증한다. 하나라도 남아있으면
-- tracking table을 지우지 않고 남은 객체 목록을 출력한 뒤 스크립트를 실패로 종료한다. 모든
-- 대상이 확인상 제거된 경우에만 tracking table을 삭제한다.
--
-- [crash-resume 정책]
-- 이 rollback 스크립트나 migration 스크립트가 중간에 중단된 뒤 재실행되는 경우, 이미 반영된
-- DDL과 아직 반영되지 않은 tracking 기록 사이에 모호함이 생길 수 있다. 이 스크립트는 그 모호함을
-- 자동으로 해소하려 하지 않는다: created_by_migration이 불확실하거나 0으로 판정된 객체는 항상
-- 삭제하지 않는 쪽을 택한다. 중단-재개 이후 완전한 rollback이 필요하면, 이 스크립트를 반복 실행해
-- 자동 정리를 시도하지 말고 backup에서 restore하거나, INFORMATION_SCHEMA를 수동으로 확인해 각
-- 객체가 이 migration이 만든 것인지 직접 판단해야 한다.

SET @dbname = DATABASE();

SET @state_table_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0001_state');
SELECT IF(@state_table_exists = 0,
  'WARNING: _migration_0001_state not found. All rollback steps below will safely no-op (created_by_migration is treated as 0 for every object). If 0001 migration was actually applied without this tracking table (older script version), this rollback cannot remove those objects automatically — verify manually with INFORMATION_SCHEMA before deciding next steps.',
  '_migration_0001_state found, proceeding with tracked rollback') AS rollback_precheck;

-- 1) library_holding.idx_library_holding_print_isbn13
SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library_holding.idx_library_holding_print_isbn13''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13');
SET @sql = IF(@created = 1 AND @idx_exists > 0,
  'ALTER TABLE library_holding DROP INDEX idx_library_holding_print_isbn13', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) library_holding table (library 컬럼보다 먼저, FK 의존)
SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library_holding''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@created = 1, 'DROP TABLE IF EXISTS library_holding', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) library 추가 index/컬럼 제거
SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.idx_library_region_status''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'idx_library_region_status');
SET @sql = IF(@created = 1 AND @idx_exists > 0, 'ALTER TABLE library DROP INDEX idx_library_region_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.uk_library_library_code''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'uk_library_library_code');
SET @sql = IF(@created = 1 AND @idx_exists > 0, 'ALTER TABLE library DROP INDEX uk_library_library_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.source''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'source');
SET @sql = IF(@created = 1 AND @exists > 0, 'ALTER TABLE library DROP COLUMN source', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.dtl_region_code''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'dtl_region_code');
SET @sql = IF(@created = 1 AND @exists > 0, 'ALTER TABLE library DROP COLUMN dtl_region_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.region_code''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'region_code');
SET @sql = IF(@created = 1 AND @exists > 0, 'ALTER TABLE library DROP COLUMN region_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.library_code''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'library_code');
SET @sql = IF(@created = 1 AND @exists > 0, 'ALTER TABLE library DROP COLUMN library_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) book 추가 index/컬럼 제거 (index 먼저, 컬럼 나중)
SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''book.idx_book_print_isbn13''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13');
SET @sql = IF(@created = 1 AND @idx_exists > 0, 'ALTER TABLE book DROP INDEX idx_book_print_isbn13', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @created = 0;
SET @sql = IF(@state_table_exists = 1,
  'SET @created = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''book.print_isbn13''), 0)',
  'SET @created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13');
SET @sql = IF(@created = 1 AND @exists > 0, 'ALTER TABLE book DROP COLUMN print_isbn13', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) 최종 검증: created_by_migration=1로 기록된 각 객체가 실제로 지금 제거됐는지
--    INFORMATION_SCHEMA로 재확인한다. 하나라도 남아있으면 @remaining에 누적한다.
SET @remaining = 0;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''book.print_isbn13''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'book.print_isbn13' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''book.idx_book_print_isbn13''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'book.idx_book_print_isbn13' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.library_code''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'library_code'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.library_code' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.uk_library_library_code''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'uk_library_library_code'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.uk_library_library_code' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.region_code''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'region_code'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.region_code' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.dtl_region_code''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'dtl_region_code'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.dtl_region_code' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.source''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'source'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.source' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library.idx_library_region_status''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'idx_library_region_status'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library.idx_library_region_status' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library_holding''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library_holding' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

SET @sql = IF(@state_table_exists = 1,
  'SET @chk = IFNULL((SELECT created_by_migration FROM _migration_0001_state WHERE object_name = ''library_holding.idx_library_holding_print_isbn13''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @still = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
SET @remaining = @remaining + IF(@chk = 1 AND @still = 1, 1, 0);
SELECT 'library_holding.idx_library_holding_print_isbn13' AS object_name, @chk AS created_by_this_migration, @still AS still_exists,
  IF(@chk = 1 AND @still = 1, 'REMAINING - rollback incomplete', 'ok') AS status;

-- 6) tracking table은 위 검증에서 남은 객체가 하나도 없을 때만 제거한다.
SET @sql = IF(@remaining = 0, 'DROP TABLE IF EXISTS _migration_0001_state', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(@remaining = 0,
  'ROLLBACK COMPLETE: all migration-created objects removed, _migration_0001_state dropped.',
  CONCAT('ROLLBACK INCOMPLETE: ', @remaining, ' migration-created object(s) still exist. _migration_0001_state was NOT dropped so the audit trail is preserved. Review the per-object report above, or query _migration_0001_state directly, before retrying.')
) AS rollback_final_result;

-- @remaining > 0이면 배치 실행 자체를 실패(0이 아닌 종료 코드)로 만든다. 최상위 SQL에서는
-- stored procedure 없이 SIGNAL을 쓸 수 없으므로, 존재하지 않는 이름의 테이블을 의도적으로
-- 조회해 MySQL 오류(1146)를 발생시켜 배치 클라이언트가 이 지점에서 중단하도록 만든다.
SET @sql = IF(@remaining > 0,
  'SELECT 1 FROM `ROLLBACK_INCOMPLETE_check_migration_0001_state`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
