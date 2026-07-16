-- PR #46 후속: admin_audit_log action_type / target_type DB CHECK 제약 수동 적용 SQL
-- 관련 계획: docs/작업현황/2026-07-14_PR46_감사로그_DB_CHECK제약_후속계획.md
-- 관련 근거: docs/작업현황/설계문서/2026-07-14_PR46_감사로그_DB_CHECK제약_분리SQL_근거.md
-- 2026-07-16 갱신: 관리자 컬렉션 API(PR #50) 추가로 AdminAuditLogActionType에
-- COLLECTION_CREATE/COLLECTION_UPDATE/COLLECTION_BOOKS_UPDATE/COLLECTION_PATCH, AdminAuditLogTargetType에
-- COLLECTION이 새로 생겼다. 이 CHECK는 schema.sql과 분리된 수동 적용 sidecar라 Java enum이
-- 바뀔 때마다 이 파일도 함께 갱신하고 재적용해야 한다 — 갱신하지 않으면 새 enum 값으로 감사
-- 로그를 기록하려는 정상 호출이 전부 CHECK 제약 위반(500)으로 실패한다(이번 라운드에서 실제로
-- AdminCollectionTransactionIntegrationTest가 이 문제로 실패하는 것을 확인하고 갱신함).
--
-- 목적:
--   Java enum / Swagger / 테스트 레벨에서는 감사 로그 표준값이 검증된다.
--   하지만 admin_audit_log.action_type, target_type은 DB에서 VARCHAR 컬럼이므로
--   수동 SQL, 운영 보정, 외부 import 경로에서는 enum 외 값이 들어갈 수 있다.
--   이 SQL은 DB 자체에서 허용값을 제한하기 위한 CHECK 제약을 추가한다.
--
-- 적용 정책:
--   - src/main/resources/schema.sql은 수정하지 않는다.
--   - 이 파일은 기존/공유/로컬 DB에 수동 적용하는 sidecar SQL이다.
--   - DDL은 MySQL에서 자동 커밋될 수 있으므로 적용 전 DB 상태를 반드시 확인한다.
--   - 아래 §1의 invalid 데이터 점검 결과가 0행일 때만 §2 이후를 진행한다.

-- §0. 현재 DB 확인
SELECT DATABASE() AS current_database;

-- §1. 적용 전 invalid 데이터 점검
-- 기대 결과: 두 쿼리 모두 0행.
-- 0행이 아니면 CHECK 제약 추가가 실패하므로, 먼저 데이터를 정상 enum 값으로 정리해야 한다.

-- §1-1. action_type invalid 데이터 점검
SELECT audit_log_id, action_type, target_type, created_at
FROM admin_audit_log
WHERE action_type NOT IN (
  'MEMBER_POINT_ADJUST',
  'COUPON_CREATE',
  'COUPON_ISSUE',
  'BOOK_CREATE',
  'CHALLENGE_UPDATE',
  'COLLECTION_CREATE',
  'COLLECTION_UPDATE',
  'COLLECTION_BOOKS_UPDATE',
  'COLLECTION_PATCH',
  'MEMBER_UPDATE',
  'BOOK_UPDATE',
  'REPORT_PROCESS',
  'NOTICE_WRITE'
);

-- §1-2. target_type invalid 데이터 점검
SELECT audit_log_id, action_type, target_type, created_at
FROM admin_audit_log
WHERE target_type NOT IN (
  'MEMBER',
  'COUPON',
  'BOOK',
  'CHALLENGE',
  'COLLECTION',
  'BOOK_REPORT',
  'REVIEW_REPORT',
  'REVIEW',
  'NOTICE',
  'SANCTION_HISTORY'
);

-- §2. 같은 이름의 CHECK 제약이 이미 있으면 제거한다.
-- MySQL 환경에 따라 DROP CHECK IF EXISTS를 사용할 수 없으므로
-- information_schema 확인 후 동적 SQL로 처리한다.

SET @constraint_name = 'chk_admin_audit_log_action_type';
SELECT COUNT(*) INTO @constraint_exists
FROM information_schema.check_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = @constraint_name;

SET @sql = IF(
  @constraint_exists > 0,
  CONCAT('ALTER TABLE admin_audit_log DROP CHECK ', @constraint_name),
  'SELECT ''skip drop chk_admin_audit_log_action_type'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_name = 'chk_admin_audit_log_target_type';
SELECT COUNT(*) INTO @constraint_exists
FROM information_schema.check_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = @constraint_name;

SET @sql = IF(
  @constraint_exists > 0,
  CONCAT('ALTER TABLE admin_audit_log DROP CHECK ', @constraint_name),
  'SELECT ''skip drop chk_admin_audit_log_target_type'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §3. CHECK 제약 추가
ALTER TABLE admin_audit_log
  ADD CONSTRAINT chk_admin_audit_log_action_type
  CHECK (action_type IN (
    'MEMBER_POINT_ADJUST',
    'COUPON_CREATE',
    'COUPON_ISSUE',
    'BOOK_CREATE',
    'CHALLENGE_UPDATE',
    'COLLECTION_CREATE',
    'COLLECTION_UPDATE',
    'COLLECTION_BOOKS_UPDATE',
    'COLLECTION_PATCH',
    'MEMBER_UPDATE',
    'BOOK_UPDATE',
    'REPORT_PROCESS',
    'NOTICE_WRITE'
  ));

ALTER TABLE admin_audit_log
  ADD CONSTRAINT chk_admin_audit_log_target_type
  CHECK (target_type IN (
    'MEMBER',
    'COUPON',
    'BOOK',
    'CHALLENGE',
    'COLLECTION',
    'BOOK_REPORT',
    'REVIEW_REPORT',
    'REVIEW',
    'NOTICE',
    'SANCTION_HISTORY'
  ));

-- §4. 적용 후 확인
-- 기대 결과: chk_admin_audit_log_action_type, chk_admin_audit_log_target_type 두 행이 조회된다.
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.check_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN (
    'chk_admin_audit_log_action_type',
    'chk_admin_audit_log_target_type'
  )
ORDER BY CONSTRAINT_NAME;

-- §5. 실패 기대 테스트(선택 실행, 로컬/롤백 가능 환경에서만)
-- 아래 INSERT는 CHECK 제약으로 실패해야 정상이다.
-- 운영/공유 DB에서는 실행하지 않는다.

-- INSERT INTO admin_audit_log (
--   admin_member_id, action_type, target_type, target_id, before_summary, after_summary, created_at
-- ) VALUES (
--   NULL, 'INVALID_ACTION', 'MEMBER', NULL, NULL, 'CHECK failure test', NOW()
-- );

-- INSERT INTO admin_audit_log (
--   admin_member_id, action_type, target_type, target_id, before_summary, after_summary, created_at
-- ) VALUES (
--   NULL, 'MEMBER_UPDATE', 'INVALID_TARGET', NULL, NULL, 'CHECK failure test', NOW()
-- );

-- §6. API 회귀 확인 안내
-- CHECK 적용 후 Swagger/Postman에서 아래 API를 1회 호출한다.
-- API 번호: API-ADMIN-AUDIT-001
-- Method: GET
-- URL: /api/v1/admin/audit-logs?page=0&size=10
-- 기대 응답: 200, success=true, 기존 감사 로그 목록 조회 가능