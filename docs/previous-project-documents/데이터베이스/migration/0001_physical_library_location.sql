-- 종이책 소장 도서관 위치 조회 기능 migration
-- 대상: book, library 테이블 컬럼 추가, library_holding 신규 테이블
-- 적용 전 절차: DB backup 필수, `SELECT VERSION();`으로 MySQL 버전 확인
-- MySQL DDL은 auto-commit이므로 이 스크립트는 여러 번 실행해도 안전(idempotent)하도록
-- INFORMATION_SCHEMA 조회 후 존재하지 않을 때만 PREPARE/EXECUTE로 ALTER TABLE을 실행한다.
-- 참고 계획: .omc/plans/physical-library-location-plan.md 5.1
--
-- [index 존재 확인 방식]
-- INFORMATION_SCHEMA.STATISTICS는 복합(composite) index일 경우 컬럼 수만큼 행이 나온다.
-- COUNT(*) = 1로 "생성됨"을 판정하면 2컬럼 이상 index는 항상 거짓으로 판정되어 tracking이
-- 틀린다(실제로는 생성했는데 created_by_migration=0으로 기록됨). 그래서 모든 index 존재 확인은
-- COUNT(*) 대신 EXISTS(SELECT 1 FROM ... WHERE ...)를 사용해 컬럼 수와 무관하게 항상 0 또는 1만
-- 반환하도록 한다. 단일/복합 index 모두 동일한 방식으로 처리한다.
--
-- [crash-resume 정책]
-- 이 스크립트가 중간에 중단된 뒤 재실행되는 경우(DDL은 성공했지만 tracking INSERT 전에
-- 프로세스가 죽는 등), 재실행 시점에는 이미 DDL이 적용된 상태이므로 exists 조건이 참이 되어
-- "이번 실행이 만들지 않았다(created_by_migration=0)"로 기록된다. 이는 의도된 동작이다: 중단
-- 시점 이전에 그 객체가 이 migration 때문에 생겼는지, 원래 있었는지 이 스크립트만으로는 구분할
-- 방법이 없기 때문에, 모호한 경우 항상 "삭제 금지"쪽으로 판정한다. 즉 crash-resume 이후에는
-- rollback이 해당 객체를 지우지 못할 수 있다. 이 상태를 벗어나려면 자동 복구를 시도하지 말고
-- backup에서 복원하거나, INFORMATION_SCHEMA를 수동으로 확인해 실제 생성 주체를 판단해야 한다.

SET @dbname = DATABASE();

-- =====================================================================
-- 0) migration state tracking table
--    rollback이 "지금 존재하는가"만으로 삭제 여부를 판단하면, migration 이전부터 우연히 같은
--    이름으로 존재하던 컬럼/테이블까지 지워버릴 수 있다. 각 대상 object별로 "이 migration이
--    실제로 만들었는가"를 기록해두고, rollback은 이 기록에 근거해서만 제거한다.
--    같은 object_name에 대해 두 번째 이후 실행에서는 INSERT IGNORE로 최초 판정값을 그대로 보존한다.
--
--    tracking 기록은 반드시 DDL을 "실행한 뒤" 실제 결과(post-state)를 재확인하고서 남긴다. DDL
--    실행 전에 미리 created_by_migration=1을 기록해두면, 그 DDL이 권한/락/디스크 문제로 실패했을 때도
--    "이 migration이 만들었다"고 잘못 기록된 상태가 남는다. 이후 DBA가 같은 이름으로 수동으로 객체를
--    만들면 rollback이 그 수동 생성 객체를 migration 산출물로 오인해 삭제할 수 있다. 그래서 아래 각
--    블록은 pre-exists 확인 -> DDL 실행 -> post-exists 재확인 -> (pre=0 AND post=1)일 때만 기록 순서를
--    지킨다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS _migration_0001_state (
  object_name VARCHAR(100) NOT NULL,
  created_by_migration TINYINT NOT NULL,
  CONSTRAINT pk_migration_0001_state PRIMARY KEY (object_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- 1) book.print_isbn13 (nullable, 종이책 ISBN. 전자책 isbn과 별도 관리)
-- =====================================================================
SET @exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'
);
SET @sql = IF(@exists = 0,
  'ALTER TABLE book ADD COLUMN print_isbn13 CHAR(13) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @post_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'
);
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('book.print_isbn13', IF(@exists = 0 AND @post_exists = 1, 1, 0));

SET @idx_exists = (
  SELECT EXISTS(
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'
  )
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE book ADD INDEX idx_book_print_isbn13 (print_isbn13)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @idx_post_exists = (
  SELECT EXISTS(
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'
  )
);
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('book.idx_book_print_isbn13', IF(@idx_exists = 0 AND @idx_post_exists = 1, 1, 0));

-- =====================================================================
-- 2) library: 수동 fallback 식별/지역 컬럼 (library_code/region_code/dtl_region_code는 nullable,
--    source는 기존 row 호환을 위해 NOT NULL DEFAULT 'MANUAL')
-- =====================================================================
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'library_code');
SET @sql = IF(@exists = 0, 'ALTER TABLE library ADD COLUMN library_code VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @post_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'library_code');
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.library_code', IF(@exists = 0 AND @post_exists = 1, 1, 0));

SET @idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'uk_library_library_code'));
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE library ADD CONSTRAINT uk_library_library_code UNIQUE (library_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_post_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'uk_library_library_code'));
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.uk_library_library_code', IF(@idx_exists = 0 AND @idx_post_exists = 1, 1, 0));

SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'region_code');
SET @sql = IF(@exists = 0, 'ALTER TABLE library ADD COLUMN region_code VARCHAR(2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @post_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'region_code');
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.region_code', IF(@exists = 0 AND @post_exists = 1, 1, 0));

SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'dtl_region_code');
SET @sql = IF(@exists = 0, 'ALTER TABLE library ADD COLUMN dtl_region_code VARCHAR(5) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @post_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'dtl_region_code');
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.dtl_region_code', IF(@exists = 0 AND @post_exists = 1, 1, 0));

SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'source');
SET @sql = IF(@exists = 0,
  'ALTER TABLE library ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT ''MANUAL''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @post_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND COLUMN_NAME = 'source');
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.source', IF(@exists = 0 AND @post_exists = 1, 1, 0));

-- library(region_code, status, dtl_region_code) 복합 index. manual fallback 조회는 항상
-- region_code + status='ACTIVE'로 필터링하고 dtl_region_code는 선택적으로만 필터링한다
-- (LibraryHoldingRepository.findManualFallback: "AND (:dtlRegionCode IS NULL OR ...)"). 항상 쓰이는
-- 두 컬럼(region_code, status)을 앞에 둬야 dtlRegion 미지정 요청에서도 index가 온전히 활용된다.
-- (region_code, dtl_region_code, status) 순서였다면 dtlRegion이 없는 요청에서는 status 조건에
-- index를 못 쓰고 해당 region 내 전체를 스캔해야 했다.
SET @idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'idx_library_region_status'));
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE library ADD INDEX idx_library_region_status (region_code, status, dtl_region_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_post_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library' AND INDEX_NAME = 'idx_library_region_status'));
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library.idx_library_region_status', IF(@idx_exists = 0 AND @idx_post_exists = 1, 1, 0));

-- =====================================================================
-- 3) library_holding: 신규 테이블. row 존재 = HELD (holding_status 컬럼 없음)
--    print_isbn13은 book에서 중복 가능한 값이라 FK를 걸지 않는다.
--    index는 CREATE TABLE에 인라인으로 넣지 않고 별도 가드된 ALTER로 추가한다: 이 migration을
--    과거 버전(index 없는 버전)으로 이미 적용한 환경에서는 CREATE TABLE IF NOT EXISTS가 아무 것도
--    하지 않아 index가 영영 추가되지 않기 때문이다.
-- =====================================================================
SET @tbl_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding');

CREATE TABLE IF NOT EXISTS library_holding (
  library_holding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  library_id BIGINT UNSIGNED NOT NULL,
  print_isbn13 CHAR(13) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  CONSTRAINT pk_library_holding PRIMARY KEY (library_holding_id),
  CONSTRAINT uk_library_holding UNIQUE (library_id, print_isbn13),
  CONSTRAINT fk_library_holding_library FOREIGN KEY (library_id) REFERENCES library(library_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @tbl_post_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding');
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library_holding', IF(@tbl_exists = 0 AND @tbl_post_exists = 1, 1, 0));

SET @idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE library_holding ADD INDEX idx_library_holding_print_isbn13 (print_isbn13, library_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_post_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
INSERT IGNORE INTO _migration_0001_state (object_name, created_by_migration)
  VALUES ('library_holding.idx_library_holding_print_isbn13', IF(@idx_exists = 0 AND @idx_post_exists = 1, 1, 0));

-- =====================================================================
-- 4) backfill: 기존 library_book -> library_holding 이관
--    print_isbn13이 유효한(숫자 13자리) book만 대상. UNIQUE 충돌은 INSERT IGNORE로 건너뜀.
-- =====================================================================
INSERT IGNORE INTO library_holding (library_id, print_isbn13, created_at, updated_at)
SELECT DISTINCT lb.library_id, b.print_isbn13, NOW(), NOW()
FROM library_book lb
JOIN book b ON b.book_id = lb.book_id
WHERE b.print_isbn13 IS NOT NULL
  AND b.print_isbn13 REGEXP '^[0-9]{13}$';

-- =====================================================================
-- 5) 재실행 안전성 확인: 위 블록을 다시 실행해도 추가 변경이 없어야 한다(idempotent).
--    _migration_0001_state의 각 행은 INSERT IGNORE로 최초 실행 시 판정이 고정되므로 재실행해도
--    값이 바뀌지 않는다.
-- =====================================================================
