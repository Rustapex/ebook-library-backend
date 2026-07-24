SELECT CASE
    WHEN COUNT(*) = 6 THEN 'ok:required_tables'
    ELSE CONCAT('fail:required_tables:', COUNT(*))
END
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('book', 'book_epub', 'library', 'library_holding', 'member', 'point_history');

SELECT CASE
    WHEN COUNT(*) = 9 THEN 'ok:required_columns'
    ELSE CONCAT('fail:required_columns:', COUNT(*))
END
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'book' AND COLUMN_NAME = 'isbn'
      AND COLUMN_TYPE = 'char(13)' AND IS_NULLABLE = 'YES')
    OR (TABLE_NAME = 'library' AND COLUMN_NAME IN
      ('library_code', 'region_code', 'dtl_region_code', 'source'))
    OR (TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'
      AND COLUMN_TYPE = 'char(13)' AND IS_NULLABLE = 'NO')
    OR (TABLE_NAME = 'member' AND COLUMN_NAME = 'current_sanction_id')
    OR (TABLE_NAME = 'point_history' AND COLUMN_NAME IN ('request_id', 'balance_after'))
  );

SELECT CASE
    WHEN COUNT(*) = 4 THEN 'ok:book_epub_columns'
    ELSE CONCAT('fail:book_epub_columns:', COUNT(*))
END
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'book_epub' AND COLUMN_NAME = 'epub_data'
      AND COLUMN_TYPE = 'longblob' AND IS_NULLABLE = 'YES')
    OR (TABLE_NAME = 'book_epub' AND COLUMN_NAME = 'storage_type'
      AND COLUMN_TYPE = 'varchar(16)' AND IS_NULLABLE = 'NO'
      AND COLUMN_DEFAULT = 'DB')
    OR (TABLE_NAME = 'book_epub' AND COLUMN_NAME = 'storage_key'
      AND COLUMN_TYPE = 'varchar(512)' AND IS_NULLABLE = 'YES')
    OR (TABLE_NAME = 'book_epub' AND COLUMN_NAME = 's3_version_id'
      AND COLUMN_TYPE = 'varchar(128)' AND IS_NULLABLE = 'YES')
  );

SELECT CASE
    WHEN COUNT(*) = 0 THEN 'ok:forbidden_transitional_columns'
    ELSE CONCAT('fail:forbidden_transitional_columns:', COUNT(*))
END
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13')
    OR (TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13')
  );

SELECT CASE
    WHEN COUNT(*) = 3 THEN 'ok:required_indexes'
    ELSE CONCAT('fail:required_indexes:', COUNT(*))
END
FROM (
    SELECT TABLE_NAME, INDEX_NAME,
           NON_UNIQUE,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND INDEX_NAME IN (
        'uk_library_holding',
        'idx_library_holding_isbn',
        'uk_point_history_member_id_request_id'
      )
    GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
    HAVING (INDEX_NAME = 'uk_library_holding' AND NON_UNIQUE = 0
            AND index_columns = 'library_id,isbn')
        OR (INDEX_NAME = 'idx_library_holding_isbn' AND NON_UNIQUE = 1
            AND index_columns = 'isbn,library_id')
        OR (INDEX_NAME = 'uk_point_history_member_id_request_id'
            AND NON_UNIQUE = 0
            AND index_columns = 'member_id,request_id')
) required_indexes;

SELECT CASE
    WHEN COUNT(*) = 1 THEN 'ok:book_epub_storage_key_unique'
    ELSE CONCAT('fail:book_epub_storage_key_unique:', COUNT(*))
END
FROM (
    SELECT INDEX_NAME, NON_UNIQUE,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'book_epub'
      AND INDEX_NAME = 'uk_book_epub_storage_key'
    GROUP BY INDEX_NAME, NON_UNIQUE
    HAVING NON_UNIQUE = 0 AND index_columns = 'storage_key'
) epub_storage_key_unique;

SELECT CASE
    WHEN COUNT(*) = 0 THEN 'ok:book_epub_row_invariants'
    ELSE CONCAT('fail:book_epub_row_invariants:', COUNT(*))
END
FROM book_epub
WHERE storage_type NOT IN ('DB', 'S3')
   OR (storage_type = 'DB' AND (epub_data IS NULL OR storage_key IS NOT NULL))
   OR (storage_type = 'S3' AND (
        storage_key IS NULL OR TRIM(storage_key) = ''
        OR s3_version_id IS NULL OR TRIM(s3_version_id) = ''
   ));
