-- 희망도서/알림/공지 구현계획: 기존/공유/로컬 DB에 수동 적용하는 migration.
-- 관련 계획: .claude/plans/iterative-rolling-garden.md
--
-- 전제조건: maintenance window(전체 application write 중단) 상태에서만 실행한다.
-- MySQL DDL은 implicit commit이 발생하므로 데이터 보정 단계와 DDL 단계를 분리했고,
-- 각 단계 사이에 검증 쿼리를 두었다. 위에서부터 순서대로, 이전 단계의 기대 결과를
-- 확인한 뒤에만 다음 단계로 진행한다.
--
-- 배포 순서: write 중단 -> 이 파일(V002) 실행 -> 신규 코드 배포/기동 -> smoke test -> write 재개.
-- 롤백은 write 재개 전에만 U002__request_notification_notice_alignment_rollback.sql 사용.
-- write 재개 이후에는 U002의 guard가 정상적으로 막으므로 roll-forward 원칙을 따른다.

-- ============================================================
-- 0. 현재 DB 확인
-- ============================================================
SELECT DATABASE() AS current_database;

-- ============================================================
-- 1. Preflight
-- ============================================================

-- 1-1. 중복 후보 그룹 수 확인(참고용, 실패 조건 아님 — 4단계에서 정리한다)
SELECT normalized_title, normalized_author, normalized_publisher, COUNT(*) AS candidate_count
FROM book_request_candidate
GROUP BY normalized_title, normalized_author, normalized_publisher
HAVING COUNT(*) > 1;

-- 1-2. notification_type 값별 건수 스냅샷(마이그레이션 후 총합 비교용)
SELECT notification_type, COUNT(*) AS cnt FROM notification GROUP BY notification_type;
SELECT COUNT(*) AS notification_total_before FROM notification;

-- 1-3. 감사 로그 CHECK 마이그레이션 전 존재 여부 확인(U002 복원 시 사용할 메타 값)
SET @action_check_existed := (
  SELECT COUNT(*) > 0 FROM information_schema.check_constraints
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_admin_audit_log_action_type'
);
SET @target_check_existed := (
  SELECT COUNT(*) > 0 FROM information_schema.check_constraints
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_admin_audit_log_target_type'
);
SELECT @action_check_existed AS action_check_existed, @target_check_existed AS target_check_existed;

-- ============================================================
-- 2. 감사 로그 CHECK preflight: 신규 값 추가 전 invalid 데이터 확인
--    기대 결과: 두 쿼리 모두 0행. 0행이 아니면 여기서 중단한다
--    (DROP 후 ADD가 실패하면 MySQL implicit commit 때문에 CHECK가 제거된 채로 남을 수 있음).
-- ============================================================
SELECT audit_log_id, action_type FROM admin_audit_log
WHERE action_type NOT IN (
  'MEMBER_POINT_ADJUST', 'COUPON_CREATE', 'COUPON_ISSUE', 'BOOK_CREATE', 'CHALLENGE_UPDATE',
  'COLLECTION_CREATE', 'COLLECTION_UPDATE', 'COLLECTION_BOOKS_UPDATE', 'COLLECTION_PATCH',
  'MEMBER_UPDATE', 'BOOK_UPDATE', 'REPORT_PROCESS', 'NOTICE_WRITE', 'BOOK_REQUEST_STATUS_UPDATE'
);

SELECT audit_log_id, target_type FROM admin_audit_log
WHERE target_type NOT IN (
  'MEMBER', 'COUPON', 'BOOK', 'CHALLENGE', 'COLLECTION', 'BOOK_REPORT', 'REVIEW_REPORT', 'REVIEW',
  'NOTICE', 'SANCTION_HISTORY', 'BOOK_REQUEST_CANDIDATE'
);

-- ============================================================
-- 3. 백업 테이블 + 메타 테이블 생성 (U002 복원용)
-- ============================================================
DROP TABLE IF EXISTS book_request_candidate_v002_backup;
CREATE TABLE book_request_candidate_v002_backup AS
SELECT * FROM book_request_candidate;

DROP TABLE IF EXISTS book_request_v002_backup_mapping;
CREATE TABLE book_request_v002_backup_mapping AS
SELECT request_id, request_candidate_id AS original_request_candidate_id FROM book_request;

DROP TABLE IF EXISTS book_request_v002_migration_meta;
CREATE TABLE book_request_v002_migration_meta (
  applied_at DATETIME NOT NULL,
  candidate_backup_row_count INT NOT NULL,
  mapping_backup_row_count INT NOT NULL,
  action_check_existed TINYINT(1) NOT NULL,
  target_check_existed TINYINT(1) NOT NULL
);

INSERT INTO book_request_v002_migration_meta
SELECT NOW(),
       (SELECT COUNT(*) FROM book_request_candidate_v002_backup),
       (SELECT COUNT(*) FROM book_request_v002_backup_mapping),
       @action_check_existed,
       @target_check_existed;

SELECT * FROM book_request_v002_migration_meta;

-- ============================================================
-- 4. 데이터 보정: 중복 후보 병합 (DDL 아님)
-- ============================================================

-- 4-1. 중복 그룹별 대표(최소 request_candidate_id) 임시 테이블
DROP TEMPORARY TABLE IF EXISTS tmp_candidate_representative;
CREATE TEMPORARY TABLE tmp_candidate_representative AS
SELECT normalized_title, normalized_author, normalized_publisher,
       MIN(request_candidate_id) AS representative_id
FROM book_request_candidate
GROUP BY normalized_title, normalized_author, normalized_publisher
HAVING COUNT(*) > 1;

-- 4-2. book_request의 request_candidate_id를 대표로 재연결
UPDATE book_request br
JOIN book_request_candidate c ON br.request_candidate_id = c.request_candidate_id
JOIN tmp_candidate_representative rep
  ON c.normalized_title = rep.normalized_title
  AND c.normalized_author = rep.normalized_author
  AND c.normalized_publisher = rep.normalized_publisher
SET br.request_candidate_id = rep.representative_id
WHERE c.request_candidate_id <> rep.representative_id;

-- 4-3. 대표 후보의 request_count 재계산
UPDATE book_request_candidate c
JOIN (
  SELECT request_candidate_id, COUNT(*) AS actual_count
  FROM book_request
  GROUP BY request_candidate_id
) actual ON c.request_candidate_id = actual.request_candidate_id
SET c.request_count = actual.actual_count;

-- 4-4. 비대표 후보 삭제
DELETE c FROM book_request_candidate c
JOIN tmp_candidate_representative rep
  ON c.normalized_title = rep.normalized_title
  AND c.normalized_author = rep.normalized_author
  AND c.normalized_publisher = rep.normalized_publisher
WHERE c.request_candidate_id <> rep.representative_id;

DROP TEMPORARY TABLE IF EXISTS tmp_candidate_representative;

-- 4-5. 검증
SELECT (SELECT COUNT(*) FROM book_request) AS book_request_count_after,
       (SELECT COUNT(*) FROM book_request_v002_backup_mapping) AS book_request_count_before;
-- 기대 결과: 두 값이 같아야 한다(병합 과정에서 book_request row 수는 변하지 않음).

SELECT normalized_title, normalized_author, normalized_publisher, COUNT(*) AS remaining_dupes
FROM book_request_candidate
GROUP BY normalized_title, normalized_author, normalized_publisher
HAVING COUNT(*) > 1;
-- 기대 결과: 0행. 0행이 아니면 5번 DDL이 실패하므로 여기서 원인을 해결해야 한다.

-- ============================================================
-- 5. DDL: candidate 정규화 컬럼 plain INDEX -> UNIQUE
-- ============================================================
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book_request_candidate'
    AND INDEX_NAME = 'idx_book_request_candidate_normalized'
);
SET @drop_idx_sql := IF(@idx_exists > 0,
  'DROP INDEX idx_book_request_candidate_normalized ON book_request_candidate',
  'SELECT ''skip drop idx_book_request_candidate_normalized'' AS message'
);
PREPARE stmt FROM @drop_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE UNIQUE INDEX idx_book_request_candidate_normalized
  ON book_request_candidate (normalized_title, normalized_author, normalized_publisher);

-- ============================================================
-- 6. notification_type 대문자 통일
-- ============================================================
UPDATE notification SET notification_type = 'PAYMENT' WHERE notification_type = 'payment';
UPDATE notification SET notification_type = 'NOTICE' WHERE notification_type = 'notice';
UPDATE notification SET notification_type = 'BOOK_REQUEST' WHERE notification_type = 'book_request';
UPDATE notification SET notification_type = 'COUPON' WHERE notification_type = 'coupon';
UPDATE notification SET notification_type = 'CHALLENGE' WHERE notification_type = 'challenge';

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
  CHECK (notification_type IN ('RENTAL', 'PAYMENT', 'NOTICE', 'EVENT', 'REPORT', 'BOOK_REQUEST', 'COUPON', 'CHALLENGE'));

-- 검증: 총 건수 불변(1-2단계 스냅샷과 비교)
SELECT COUNT(*) AS notification_total_after FROM notification;

-- ============================================================
-- 7. 감사 로그 CHECK: 마이그레이션 전 존재했던 것만 DROP 후 신규 값 포함 재생성
--    존재하지 않았던 DB는 이번에도 추가하지 않는다(U002에서 대칭적으로 복원 가능하도록).
-- ============================================================
SET @action_drop_sql := IF(@action_check_existed = 1,
  'ALTER TABLE admin_audit_log DROP CHECK chk_admin_audit_log_action_type',
  'SELECT ''skip: action check did not exist before V002'' AS message'
);
PREPARE stmt FROM @action_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @action_add_sql := IF(@action_check_existed = 1,
  'ALTER TABLE admin_audit_log ADD CONSTRAINT chk_admin_audit_log_action_type CHECK (action_type IN (''MEMBER_POINT_ADJUST'',''COUPON_CREATE'',''COUPON_ISSUE'',''BOOK_CREATE'',''CHALLENGE_UPDATE'',''COLLECTION_CREATE'',''COLLECTION_UPDATE'',''COLLECTION_BOOKS_UPDATE'',''COLLECTION_PATCH'',''MEMBER_UPDATE'',''BOOK_UPDATE'',''REPORT_PROCESS'',''NOTICE_WRITE'',''BOOK_REQUEST_STATUS_UPDATE''))',
  'SELECT ''skip: action check not (re)added'' AS message'
);
PREPARE stmt FROM @action_add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @target_drop_sql := IF(@target_check_existed = 1,
  'ALTER TABLE admin_audit_log DROP CHECK chk_admin_audit_log_target_type',
  'SELECT ''skip: target check did not exist before V002'' AS message'
);
PREPARE stmt FROM @target_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @target_add_sql := IF(@target_check_existed = 1,
  'ALTER TABLE admin_audit_log ADD CONSTRAINT chk_admin_audit_log_target_type CHECK (target_type IN (''MEMBER'',''COUPON'',''BOOK'',''CHALLENGE'',''COLLECTION'',''BOOK_REPORT'',''REVIEW_REPORT'',''REVIEW'',''NOTICE'',''SANCTION_HISTORY'',''BOOK_REQUEST_CANDIDATE''))',
  'SELECT ''skip: target check not (re)added'' AS message'
);
PREPARE stmt FROM @target_add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 8. Post-apply 검증
-- ============================================================

-- 8-1. FK 무결성: book_request가 가리키는 후보가 실제로 존재하는지
SELECT COUNT(*) AS orphan_book_request
FROM book_request br
LEFT JOIN book_request_candidate c ON br.request_candidate_id = c.request_candidate_id
WHERE c.request_candidate_id IS NULL;
-- 기대 결과: 0

-- 8-2. request_count 일치 검증
SELECT c.request_candidate_id, c.request_count, actual.actual_count
FROM book_request_candidate c
JOIN (
  SELECT request_candidate_id, COUNT(*) AS actual_count
  FROM book_request GROUP BY request_candidate_id
) actual ON c.request_candidate_id = actual.request_candidate_id
WHERE c.request_count <> actual.actual_count;
-- 기대 결과: 0행

-- 8-3. 감사 로그 CHECK self-cleaning smoke test.
--      반드시 COMMIT하지 않는다 — 커밋된 row가 남으면 U002 guard(신규값 사용 row 0건 확인)가
--      정상 상황에서도 항상 실패해 rollback이 영구히 막힌다. action_check_existed=0인 DB는
--      CHECK 자체가 없으므로 이 smoke test를 생략한다.
SELECT @action_check_existed AS run_smoke_test_if_1;
-- 아래 3줄은 @action_check_existed = 1 일 때만 수동으로 실행한다(동적 분기 대상 아님 — 트랜잭션 문은 PREPARE로 감쌀 수 없음).
-- START TRANSACTION;
-- INSERT INTO admin_audit_log (admin_member_id, action_type, target_type, target_id, before_summary, after_summary, created_at)
--   VALUES (NULL, 'BOOK_REQUEST_STATUS_UPDATE', 'BOOK_REQUEST_CANDIDATE', NULL, NULL, 'V002 smoke test', NOW());
-- ROLLBACK;

-- ============================================================
-- V002 적용 완료. 다음: 신규 코드 배포/기동 -> smoke test(11개 엔드포인트) -> write 재개.
-- ============================================================
