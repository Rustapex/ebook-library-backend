CREATE TABLE IF NOT EXISTS book_epub (
  book_epub_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  book_id BIGINT UNSIGNED NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT UNSIGNED NOT NULL,
  sha256 CHAR(64) NOT NULL,
  epub_version INT UNSIGNED NOT NULL DEFAULT 1,
  epub_data LONGBLOB NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  CONSTRAINT pk_book_epub PRIMARY KEY (book_epub_id),
  CONSTRAINT uk_book_epub_book_version UNIQUE (book_id, epub_version),
  CONSTRAINT fk_book_epub_book FOREIGN KEY (book_id) REFERENCES book(book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @has_book_epub_surrogate_id := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'book_epub'
    AND column_name = 'book_epub_id'
);
SET @book_epub_pk_sql := IF(
  @has_book_epub_surrogate_id = 0,
  'ALTER TABLE book_epub DROP PRIMARY KEY, ADD COLUMN book_epub_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT FIRST, ADD CONSTRAINT pk_book_epub PRIMARY KEY (book_epub_id), ADD INDEX idx_book_epub_book_id (book_id)',
  'SELECT 1'
);
PREPARE book_epub_pk_stmt FROM @book_epub_pk_sql;
EXECUTE book_epub_pk_stmt;
DEALLOCATE PREPARE book_epub_pk_stmt;

SET @has_book_epub_version_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'book_epub'
    AND index_name = 'uk_book_epub_book_version'
);
SET @book_epub_unique_sql := IF(
  @has_book_epub_version_unique = 0,
  'ALTER TABLE book_epub ADD CONSTRAINT uk_book_epub_book_version UNIQUE (book_id, epub_version)',
  'SELECT 1'
);
PREPARE book_epub_unique_stmt FROM @book_epub_unique_sql;
EXECUTE book_epub_unique_stmt;
DEALLOCATE PREPARE book_epub_unique_stmt;

ALTER TABLE reading_progress
  ADD COLUMN epub_version INT UNSIGNED NOT NULL DEFAULT 1 AFTER read_page_count,
  ADD COLUMN location_cfi TEXT NULL AFTER epub_version,
  ADD COLUMN chapter_href VARCHAR(1000) NULL AFTER location_cfi,
  ADD COLUMN spine_index INT UNSIGNED NULL AFTER chapter_href,
  ADD COLUMN progression DECIMAL(6,5) NOT NULL DEFAULT 0 AFTER spine_index,
  ADD COLUMN revision BIGINT NOT NULL DEFAULT 0 AFTER progression,
  ADD CONSTRAINT chk_reading_progress_progression CHECK (progression >= 0 AND progression <= 1);

ALTER TABLE bookmark
  MODIFY COLUMN page_no INT UNSIGNED NULL,
  ADD COLUMN epub_version INT UNSIGNED NULL AFTER page_no,
  ADD COLUMN location_cfi TEXT NULL AFTER epub_version,
  ADD COLUMN location_hash BINARY(32) NULL AFTER location_cfi,
  ADD COLUMN chapter_href VARCHAR(1000) NULL AFTER location_hash,
  ADD COLUMN spine_index INT UNSIGNED NULL AFTER chapter_href,
  ADD COLUMN progression DECIMAL(6,5) NULL AFTER spine_index,
  ADD CONSTRAINT uk_bookmark_rental_epub_location
    UNIQUE (rental_id, epub_version, location_hash),
  ADD CONSTRAINT chk_bookmark_progression
    CHECK (progression IS NULL OR (progression >= 0 AND progression <= 1));
