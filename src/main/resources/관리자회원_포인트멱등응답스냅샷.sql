-- PR51 코드리뷰 대응 계획(2026-07-18) §2.1 — schema.sql 미반영 sidecar SQL
-- 관련 커밋: fix(admin-member) PR51 코드리뷰 반영 — 포인트 보호 대상 및 멱등 재응답 보완
--
-- 적용 정책:
--   - 기존 sidecar(관리자회원_계정정지연결및포인트멱등키.sql)는 수정하지 않고, 이 파일로 분리한다.
--   - point_history.balance_after: 관리자 포인트 조정(API-ADMIN-MEMBER-004) 조정 직후 잔액 스냅샷.
--     기존 이력은 NULL로 유지하고, 신규 ADMIN_ADJUST 이력만 서비스에서 값 저장을 강제한다.
--   - DDL은 MySQL에서 자동 커밋될 수 있으므로 적용 전 DB 상태를 반드시 확인한다.

-- §0. 현재 DB 확인
SELECT DATABASE() AS current_database;

-- §1. point_history.balance_after 컬럼 추가 (이미 있으면 스킵)
SELECT COUNT(*) INTO @column_exists
FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'point_history'
  AND COLUMN_NAME = 'balance_after';

SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE point_history ADD COLUMN balance_after INT NULL AFTER request_id',
  'SELECT ''skip add point_history.balance_after'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §2. 적용 후 확인
-- 기대 결과: balance_after 컬럼이 조회된다.
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DATA_TYPE
FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'point_history'
  AND COLUMN_NAME = 'balance_after';
