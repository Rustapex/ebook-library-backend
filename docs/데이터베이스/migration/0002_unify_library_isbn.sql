-- 도서관 조회 ISBN 단일화 migration
-- 목표: Book.isbn을 유일한 조회 기준으로 사용하고 0001의 별도 print ISBN 구조를 정리한다.
-- 적용 전 DB backup 필수. MySQL DDL은 auto-commit이며 이 파일은 여러 번 실행해도 안전해야 한다.

SET @dbname = DATABASE();

SET @state_pre_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0002_state'));
CREATE TABLE IF NOT EXISTS _migration_0002_state (
  object_name VARCHAR(100) NOT NULL,
  changed_by_migration TINYINT NOT NULL,
  CONSTRAINT pk_migration_0002_state PRIMARY KEY (object_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('_migration_0002_state', IF(@state_pre_exists = 0, 1, 0));

-- book.print_isbn13 제거 전 원래 값을 보존한다. rollback이 완료되면 함께 제거한다.
SET @backup_pre_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0002_book_print_backup'));
CREATE TABLE IF NOT EXISTS _migration_0002_book_print_backup (
  book_id BIGINT UNSIGNED NOT NULL,
  print_isbn13 CHAR(13) NULL,
  CONSTRAINT pk_migration_0002_book_print_backup PRIMARY KEY (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
SET @backup_post_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0002_book_print_backup'));
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('_migration_0002_book_print_backup',
          IF(@backup_pre_exists = 0 AND @backup_post_exists = 1, 1, 0));

SET @book_print_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @sql = IF(@book_print_exists = 1,
  'INSERT IGNORE INTO _migration_0002_book_print_backup (book_id, print_isbn13) SELECT book_id, print_isbn13 FROM book',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- library_holding.print_isbn13 -> isbn
SET @holding_old_col = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13'));
SET @holding_new_col = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'));
SET @sql = IF(@holding_old_col = 1 AND @holding_new_col = 0,
  'ALTER TABLE library_holding RENAME COLUMN print_isbn13 TO isbn',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @holding_new_post = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'));
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('library_holding.column_renamed_to_isbn',
          IF(@holding_old_col = 1 AND @holding_new_col = 0 AND @holding_new_post = 1, 1, 0));

-- secondary index 이름도 현재 컬럼명에 맞춘다.
SET @holding_old_idx = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'
    AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
SET @holding_new_idx = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'
    AND INDEX_NAME = 'idx_library_holding_isbn'));
SET @sql = IF(@holding_old_idx = 1 AND @holding_new_idx = 0,
  'ALTER TABLE library_holding RENAME INDEX idx_library_holding_print_isbn13 TO idx_library_holding_isbn',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @holding_new_idx_post = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'
    AND INDEX_NAME = 'idx_library_holding_isbn'));
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('library_holding.index_renamed_to_isbn',
          IF(@holding_old_idx = 1 AND @holding_new_idx = 0 AND @holding_new_idx_post = 1, 1, 0));

-- 기존 library_book 관계가 있으면 Book.isbn 기준으로 보강한다.
INSERT IGNORE INTO library_holding (library_id, isbn, created_at, updated_at)
SELECT DISTINCT lb.library_id, b.isbn, NOW(), NOW()
FROM library_book lb
JOIN book b ON b.book_id = lb.book_id
WHERE b.isbn IS NOT NULL
  AND b.isbn REGEXP '^[0-9]{13}$';

-- 더 이상 사용하지 않는 book.print_isbn13 index/column 제거(index 먼저).
SET @book_old_idx = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
SET @sql = IF(@book_old_idx = 1,
  'ALTER TABLE book DROP INDEX idx_book_print_isbn13',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @book_old_idx_post = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('book.idx_book_print_isbn13_dropped',
          IF(@book_old_idx = 1 AND @book_old_idx_post = 0, 1, 0));

SET @book_old_col = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @sql = IF(@book_old_col = 1,
  'ALTER TABLE book DROP COLUMN print_isbn13',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @book_old_col_post = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
INSERT IGNORE INTO _migration_0002_state (object_name, changed_by_migration)
  VALUES ('book.print_isbn13_dropped',
          IF(@book_old_col = 1 AND @book_old_col_post = 0, 1, 0));

-- 최종 구조 검증
SET @holding_isbn_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'));
SET @holding_print_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13'));
SET @holding_idx_columns = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'
    AND INDEX_NAME = 'idx_library_holding_isbn');
SET @holding_uk_columns = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding'
    AND INDEX_NAME = 'uk_library_holding');
SET @book_print_remaining = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @book_print_idx_remaining = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
SET @invalid_holding = (SELECT COUNT(*) FROM library_holding
  WHERE isbn IS NULL OR isbn NOT REGEXP '^[0-9]{13}$');

SET @validation_errors =
    IF(@holding_isbn_exists = 1, 0, 1)
  + IF(@holding_print_exists = 0, 0, 1)
  + IF(@holding_idx_columns = 'isbn,library_id', 0, 1)
  + IF(@holding_uk_columns = 'library_id,isbn', 0, 1)
  + IF(@book_print_remaining = 0, 0, 1)
  + IF(@book_print_idx_remaining = 0, 0, 1)
  + IF(@invalid_holding = 0, 0, 1);

SELECT @validation_errors AS validation_errors,
       @holding_idx_columns AS holding_index_columns,
       @holding_uk_columns AS holding_unique_columns,
       @invalid_holding AS invalid_holding_rows;
SET @sql = IF(@validation_errors = 0,
  'SELECT ''0002_unify_library_isbn migration complete'' AS result',
  'SELECT * FROM _migration_0002_validation_failed');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
