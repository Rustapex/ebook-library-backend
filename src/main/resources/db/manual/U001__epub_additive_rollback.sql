ALTER TABLE bookmark
  DROP CHECK chk_bookmark_progression,
  DROP INDEX uk_bookmark_rental_epub_location,
  DROP COLUMN progression,
  DROP COLUMN spine_index,
  DROP COLUMN chapter_href,
  DROP COLUMN location_hash,
  DROP COLUMN location_cfi,
  DROP COLUMN epub_version;

DELETE FROM bookmark WHERE page_no IS NULL;
ALTER TABLE bookmark MODIFY COLUMN page_no INT UNSIGNED NOT NULL;

ALTER TABLE reading_progress
  DROP CHECK chk_reading_progress_progression,
  DROP COLUMN revision,
  DROP COLUMN progression,
  DROP COLUMN spine_index,
  DROP COLUMN chapter_href,
  DROP COLUMN location_cfi,
  DROP COLUMN epub_version;

DROP TABLE book_epub;
