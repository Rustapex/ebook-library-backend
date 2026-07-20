-- V002__request_notification_notice_alignment.sql 롤백.
-- 전제조건: write 재개 "전"에만 실행한다. write 재개 이후에는 사용하지 않는다 — 이 파일의 guard가
-- 재개 이후 유입된 신규 데이터를 감지해 정상적으로 실행을 거부하도록 설계되어 있으며, 그 시점부터는
-- 데이터를 되돌리는 대신 roll-forward(원인 수정 후 재배포) 원칙을 따른다.

SELECT DATABASE() AS current_database;

-- ============================================================
-- 0. 전체 guard: 하나라도 기대와 다르면 SIGNAL로 즉시 중단하고 1번 이후를 실행하지 않는다.
--    SIGNAL은 저장 프로시저 안에서만 쓸 수 있어 임시 프로시저로 감싼다 — mysql 클라이언트/SOURCE는
--    기본적으로 처리되지 않은 오류를 만나면 이후 문장을 실행하지 않고 중단한다.
-- ============================================================

-- 0-1. maintenance mode 활성 여부는 애플리케이션 레벨에서 수동 확인한다(운영 절차 문서 참고).
--      이 스크립트는 DB 레벨 가드만 자동화한다.

DROP PROCEDURE IF EXISTS sp_u002_guard;

DELIMITER $$
CREATE PROCEDURE sp_u002_guard()
BEGIN
  DECLARE meta_exists INT;
  DECLARE candidate_backup_expected INT;
  DECLARE candidate_backup_actual INT;
  DECLARE mapping_backup_expected INT;
  DECLARE mapping_backup_actual INT;
  DECLARE new_action_value_rows INT;
  DECLARE new_target_value_rows INT;
  DECLARE new_book_request_rows INT;
  DECLARE new_candidate_rows INT;

  -- 0-2. 메타 테이블 존재 확인. 0이면 V002가 적용되지 않았거나 이미 정리된 상태이므로 중단한다.
  SELECT COUNT(*) INTO meta_exists FROM information_schema.tables
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book_request_v002_migration_meta';

  IF meta_exists = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: book_request_v002_migration_meta not found. V002 was not applied or was already cleaned up.';
  END IF;

  -- 0-3. 백업 테이블 row count가 메타 기록과 일치하는지 확인(단순 "row count > 0"이 아님 —
  --      원본 데이터가 0건이었던 정상 DB도 있으므로 "기록값과 일치"가 기준이다)
  SELECT candidate_backup_row_count, mapping_backup_row_count
    INTO candidate_backup_expected, mapping_backup_expected
  FROM book_request_v002_migration_meta LIMIT 1;

  SELECT COUNT(*) INTO candidate_backup_actual FROM book_request_candidate_v002_backup;
  SELECT COUNT(*) INTO mapping_backup_actual FROM book_request_v002_backup_mapping;

  IF candidate_backup_expected <> candidate_backup_actual THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: candidate backup row count does not match the value recorded at V002 time. Backup table may be damaged.';
  END IF;

  IF mapping_backup_expected <> mapping_backup_actual THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: mapping backup row count does not match the value recorded at V002 time. Backup table may be damaged.';
  END IF;

  -- 0-4. 신규 감사로그 값 사용 row 0건 확인. 0이 아니면 write 재개 이후 정상적으로 쌓인 데이터가 있다는 뜻이므로 중단.
  SELECT COUNT(*) INTO new_action_value_rows FROM admin_audit_log WHERE action_type = 'BOOK_REQUEST_STATUS_UPDATE';
  IF new_action_value_rows > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: admin_audit_log already has rows using BOOK_REQUEST_STATUS_UPDATE. Rolling back now would hide real audit data.';
  END IF;

  SELECT COUNT(*) INTO new_target_value_rows FROM admin_audit_log WHERE target_type = 'BOOK_REQUEST_CANDIDATE';
  IF new_target_value_rows > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: admin_audit_log already has rows using BOOK_REQUEST_CANDIDATE. Rolling back now would hide real audit data.';
  END IF;

  -- 0-5. anti-join: V002 이후 신규 쓰기 여부 확인
  -- (candidate에 createdAt 컬럼이 없고 백업에 watermark도 없어 "최신 생성 시각 비교"는 불가능하므로 이 방식 사용)
  SELECT COUNT(*) INTO new_book_request_rows
  FROM book_request br
  LEFT JOIN book_request_v002_backup_mapping m ON br.request_id = m.request_id
  WHERE m.request_id IS NULL;

  IF new_book_request_rows > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: book_request has rows created after V002. Rolling back now would lose this data.';
  END IF;

  SELECT COUNT(*) INTO new_candidate_rows
  FROM book_request_candidate c
  LEFT JOIN book_request_candidate_v002_backup b ON c.request_candidate_id = b.request_candidate_id
  WHERE b.request_candidate_id IS NULL;

  IF new_candidate_rows > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'U002 guard failed: book_request_candidate has rows created after V002. Rolling back now would lose this data.';
  END IF;
END$$
DELIMITER ;

CALL sp_u002_guard();
DROP PROCEDURE sp_u002_guard;

-- 위 guard를 통과했을 때만(프로시저가 SIGNAL 없이 정상 종료했을 때만) 아래 1번부터 진행된다.

-- ============================================================
-- 1. 정규화 UNIQUE 제약 제거
--    (제거하지 않으면 3번에서 중복 후보를 원래 ID로 복원할 수 없다)
-- ============================================================
SET @uidx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book_request_candidate'
    AND INDEX_NAME = 'idx_book_request_candidate_normalized'
);
SET @drop_uidx_sql := IF(@uidx_exists > 0,
  'DROP INDEX idx_book_request_candidate_normalized ON book_request_candidate',
  'SELECT ''skip drop idx_book_request_candidate_normalized'' AS message'
);
PREPARE stmt FROM @drop_uidx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 삭제됐던 candidate를 원래 ID로 복원
-- ============================================================
INSERT INTO book_request_candidate (
  request_candidate_id, normalized_title, normalized_author, normalized_publisher,
  request_count, first_requested_at, candidate_status
)
SELECT b.request_candidate_id, b.normalized_title, b.normalized_author, b.normalized_publisher,
       b.request_count, b.first_requested_at, b.candidate_status
FROM book_request_candidate_v002_backup b
LEFT JOIN book_request_candidate c ON b.request_candidate_id = c.request_candidate_id
WHERE c.request_candidate_id IS NULL;

-- ============================================================
-- 3. book_request.request_candidate_id를 백업 매핑 기준으로 원복
-- ============================================================
UPDATE book_request br
JOIN book_request_v002_backup_mapping m ON br.request_id = m.request_id
SET br.request_candidate_id = m.original_request_candidate_id
WHERE br.request_candidate_id <> m.original_request_candidate_id;

-- ============================================================
-- 4. candidate 전체 속성 백업 기준 원복(상태/request_count/first_requested_at 등)
-- ============================================================
UPDATE book_request_candidate c
JOIN book_request_candidate_v002_backup b ON c.request_candidate_id = b.request_candidate_id
SET c.normalized_title = b.normalized_title,
    c.normalized_author = b.normalized_author,
    c.normalized_publisher = b.normalized_publisher,
    c.request_count = b.request_count,
    c.first_requested_at = b.first_requested_at,
    c.candidate_status = b.candidate_status;

-- ============================================================
-- 5. 원래 plain INDEX 재생성
-- ============================================================
CREATE INDEX idx_book_request_candidate_normalized
  ON book_request_candidate (normalized_title, normalized_author, normalized_publisher);

-- ============================================================
-- 6. notification_type 값/CHECK 원상복구
-- ============================================================
UPDATE notification SET notification_type = 'payment' WHERE notification_type = 'PAYMENT';
UPDATE notification SET notification_type = 'notice' WHERE notification_type = 'NOTICE';
UPDATE notification SET notification_type = 'book_request' WHERE notification_type = 'BOOK_REQUEST';
UPDATE notification SET notification_type = 'coupon' WHERE notification_type = 'COUPON';
UPDATE notification SET notification_type = 'challenge' WHERE notification_type = 'CHALLENGE';

SET @notif_check_exists := (
  SELECT COUNT(*) FROM information_schema.check_constraints
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_notification_notification_type'
);
SET @drop_notif_check_sql := IF(@notif_check_exists > 0,
  'ALTER TABLE notification DROP CHECK chk_notification_notification_type',
  'SELECT ''skip drop chk_notification_notification_type'' AS message'
);
PREPARE stmt FROM @drop_notif_check_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE notification
  ADD CONSTRAINT chk_notification_notification_type
  CHECK (notification_type IN ('RENTAL', 'payment', 'notice', 'EVENT', 'REPORT', 'book_request', 'coupon', 'challenge'));

-- ============================================================
-- 7. 감사 로그 CHECK: 메타의 존재 플래그가 1이었던 것만 원래 값으로 복원.
--    0이었던(V002 이전에도 없던) 것은 그대로 "없는 상태"로 둔다.
-- ============================================================
SET @action_check_existed := (SELECT action_check_existed FROM book_request_v002_migration_meta LIMIT 1);
SET @target_check_existed := (SELECT target_check_existed FROM book_request_v002_migration_meta LIMIT 1);

SET @action_drop_sql := IF(@action_check_existed = 1,
  'ALTER TABLE admin_audit_log DROP CHECK chk_admin_audit_log_action_type',
  'SELECT ''skip: action check was not present before V002, leaving as-is'' AS message'
);
PREPARE stmt FROM @action_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @action_restore_sql := IF(@action_check_existed = 1,
  'ALTER TABLE admin_audit_log ADD CONSTRAINT chk_admin_audit_log_action_type CHECK (action_type IN (''MEMBER_POINT_ADJUST'',''COUPON_CREATE'',''COUPON_ISSUE'',''BOOK_CREATE'',''CHALLENGE_UPDATE'',''COLLECTION_CREATE'',''COLLECTION_UPDATE'',''COLLECTION_BOOKS_UPDATE'',''COLLECTION_PATCH'',''MEMBER_UPDATE'',''BOOK_UPDATE'',''REPORT_PROCESS'',''NOTICE_WRITE''))',
  'SELECT ''skip: action check not restored'' AS message'
);
PREPARE stmt FROM @action_restore_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @target_drop_sql := IF(@target_check_existed = 1,
  'ALTER TABLE admin_audit_log DROP CHECK chk_admin_audit_log_target_type',
  'SELECT ''skip: target check was not present before V002, leaving as-is'' AS message'
);
PREPARE stmt FROM @target_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @target_restore_sql := IF(@target_check_existed = 1,
  'ALTER TABLE admin_audit_log ADD CONSTRAINT chk_admin_audit_log_target_type CHECK (target_type IN (''MEMBER'',''COUPON'',''BOOK'',''CHALLENGE'',''COLLECTION'',''BOOK_REPORT'',''REVIEW_REPORT'',''REVIEW'',''NOTICE'',''SANCTION_HISTORY''))',
  'SELECT ''skip: target check not restored'' AS message'
);
PREPARE stmt FROM @target_restore_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 8. 검증
-- ============================================================
SELECT normalized_title, normalized_author, normalized_publisher, COUNT(*) AS dupe_after_rollback
FROM book_request_candidate
GROUP BY normalized_title, normalized_author, normalized_publisher
HAVING COUNT(*) > 1;
-- 참고: V002 이전에 있던 중복이 다시 나타나는 것은 정상이다(원본 상태로 복원했으므로).

SELECT COUNT(*) AS orphan_book_request
FROM book_request br
LEFT JOIN book_request_candidate c ON br.request_candidate_id = c.request_candidate_id
WHERE c.request_candidate_id IS NULL;
-- 기대 결과: 0

-- ============================================================
-- 9. 검증 통과 후에만 수동으로 백업/메타 테이블 정리(자동 실행 안 함)
-- ============================================================
-- DROP TABLE book_request_candidate_v002_backup;
-- DROP TABLE book_request_v002_backup_mapping;
-- DROP TABLE book_request_v002_migration_meta;
