-- 0002_unify_library_isbn 보상 rollback
-- MySQL DDL은 auto-commit이다. 실행 전 backup 필수.
-- 0002에서 Book.isbn 기준으로 추가한 library_holding row는 데이터 손실 방지를 위해 삭제하지 않는다.

SET @dbname = DATABASE();
SET @state_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0002_state'));

SET @changed = 0;
SET @sql = IF(@state_exists = 1,
  'SET @changed = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''book.print_isbn13_dropped''), 0)',
  'SET @changed = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @book_print_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @sql = IF(@changed = 1 AND @book_print_exists = 0,
  'ALTER TABLE book ADD COLUMN print_isbn13 CHAR(13) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 보존했던 원래 값을 복원한다.
SET @backup_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = '_migration_0002_book_print_backup'));
SET @book_print_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @sql = IF(@backup_exists = 1 AND @book_print_exists = 1,
  'UPDATE book b JOIN _migration_0002_book_print_backup bk ON bk.book_id = b.book_id SET b.print_isbn13 = bk.print_isbn13',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @changed = 0;
SET @sql = IF(@state_exists = 1,
  'SET @changed = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''book.idx_book_print_isbn13_dropped''), 0)',
  'SET @changed = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @book_idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
SET @sql = IF(@changed = 1 AND @book_idx_exists = 0,
  'ALTER TABLE book ADD INDEX idx_book_print_isbn13 (print_isbn13)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- holding index 이름을 0001 상태로 복원한다.
SET @changed = 0;
SET @sql = IF(@state_exists = 1,
  'SET @changed = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''library_holding.index_renamed_to_isbn''), 0)',
  'SET @changed = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @new_idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_isbn'));
SET @old_idx_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
SET @sql = IF(@changed = 1 AND @new_idx_exists = 1 AND @old_idx_exists = 0,
  'ALTER TABLE library_holding RENAME INDEX idx_library_holding_isbn TO idx_library_holding_print_isbn13',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- holding column 이름을 0001 상태로 복원한다.
SET @changed = 0;
SET @sql = IF(@state_exists = 1,
  'SET @changed = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''library_holding.column_renamed_to_isbn''), 0)',
  'SET @changed = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @new_col_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'));
SET @old_col_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13'));
SET @sql = IF(@changed = 1 AND @new_col_exists = 1 AND @old_col_exists = 0,
  'ALTER TABLE library_holding RENAME COLUMN isbn TO print_isbn13',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- migration이 실제로 바꾼 객체만 0001 상태로 복원됐는지 검증한다.
SET @remaining = 0;

SET @sql = IF(@state_exists = 1,
  'SET @chk = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''book.print_isbn13_dropped''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'));
SET @remaining = @remaining + IF(@chk = 1 AND @exists = 0, 1, 0);

SET @sql = IF(@state_exists = 1,
  'SET @chk = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''book.idx_book_print_isbn13_dropped''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'book' AND INDEX_NAME = 'idx_book_print_isbn13'));
SET @remaining = @remaining + IF(@chk = 1 AND @exists = 0, 1, 0);

SET @sql = IF(@state_exists = 1,
  'SET @chk = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''library_holding.column_renamed_to_isbn''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @old_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13'));
SET @new_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'));
SET @remaining = @remaining + IF(@chk = 1 AND (@old_exists = 0 OR @new_exists = 1), 1, 0);

SET @sql = IF(@state_exists = 1,
  'SET @chk = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''library_holding.index_renamed_to_isbn''), 0)',
  'SET @chk = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @old_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_print_isbn13'));
SET @new_exists = (SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'library_holding' AND INDEX_NAME = 'idx_library_holding_isbn'));
SET @remaining = @remaining + IF(@chk = 1 AND (@old_exists = 0 OR @new_exists = 1), 1, 0);

SELECT @remaining AS rollback_remaining_objects;
SET @backup_created = 0;
SET @sql = IF(@state_exists = 1,
  'SET @backup_created = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''_migration_0002_book_print_backup''), 0)',
  'SET @backup_created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @state_created = 0;
SET @sql = IF(@state_exists = 1,
  'SET @state_created = IFNULL((SELECT changed_by_migration FROM _migration_0002_state WHERE object_name = ''_migration_0002_state''), 0)',
  'SET @state_created = 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@remaining = 0 AND @backup_created = 1,
  'DROP TABLE IF EXISTS _migration_0002_book_print_backup',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@remaining = 0 AND @state_created = 1,
  'DROP TABLE IF EXISTS _migration_0002_state',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@remaining = 0,
  'SELECT ''0002_unify_library_isbn rollback complete'' AS result',
  'SELECT * FROM _migration_0002_rollback_incomplete');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
