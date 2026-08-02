-- 관리자 회원 API 구현계획(2026-07-16) — schema.sql 미반영 sidecar SQL
-- 관련 커밋: feat(admin-member) 관리자 회원 API 4종 및 계정정지 만료 배치 추가
--
-- 적용 정책:
--   - src/main/resources/schema.sql은 CREATE TABLE IF NOT EXISTS라 기존 DB에는
--     신규 컬럼이 자동 반영되지 않고, 신규 DB 기준으로만 유지한다는 방침에 따라
--     schema.sql 자체는 수정하지 않는다.
--   - 이 파일은 기존/공유/로컬 DB에 수동 적용하는 sidecar SQL이다.
--   - member.current_sanction_id: 현재 SANCTIONED 상태의 원인이 된 계정 정지
--     이력(sanction_history) 연결값.
--   - point_history.request_id + UNIQUE(member_id, request_id): 관리자 포인트
--     조정(API-ADMIN-MEMBER-004) 요청의 멱등 키.
--   - DDL은 MySQL에서 자동 커밋될 수 있으므로 적용 전 DB 상태를 반드시 확인한다.

-- §0. 현재 DB 확인
SELECT DATABASE() AS current_database;

-- §1. member.current_sanction_id 컬럼 추가 (이미 있으면 스킵)
SELECT COUNT(*) INTO @column_exists
FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'member'
  AND COLUMN_NAME = 'current_sanction_id';

SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE member ADD COLUMN current_sanction_id BIGINT UNSIGNED NULL AFTER withdrawal_reason',
  'SELECT ''skip add member.current_sanction_id'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §2. member.current_sanction_id FK 추가 (이미 있으면 스킵)
SELECT COUNT(*) INTO @constraint_exists
FROM information_schema.table_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'member'
  AND CONSTRAINT_NAME = 'fk_member_current_sanction';

SET @sql = IF(
  @constraint_exists = 0,
  'ALTER TABLE member ADD CONSTRAINT fk_member_current_sanction FOREIGN KEY (current_sanction_id) REFERENCES sanction_history(sanction_history_id)',
  'SELECT ''skip add fk_member_current_sanction'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §3. point_history.request_id 컬럼 추가 (이미 있으면 스킵)
SELECT COUNT(*) INTO @column_exists
FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'point_history'
  AND COLUMN_NAME = 'request_id';

SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE point_history ADD COLUMN request_id CHAR(36) NULL AFTER description',
  'SELECT ''skip add point_history.request_id'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §4. point_history UNIQUE(member_id, request_id) 추가 (이미 있으면 스킵)
SELECT COUNT(*) INTO @constraint_exists
FROM information_schema.table_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'point_history'
  AND CONSTRAINT_NAME = 'uk_point_history_member_id_request_id';

SET @sql = IF(
  @constraint_exists = 0,
  'ALTER TABLE point_history ADD CONSTRAINT uk_point_history_member_id_request_id UNIQUE (member_id, request_id)',
  'SELECT ''skip add uk_point_history_member_id_request_id'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- §5. 적용 후 확인
-- 기대 결과: current_sanction_id, request_id 두 컬럼이 조회된다.
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DATA_TYPE
FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'member' AND COLUMN_NAME = 'current_sanction_id')
    OR (TABLE_NAME = 'point_history' AND COLUMN_NAME = 'request_id'));

-- 기대 결과: fk_member_current_sanction, uk_point_history_member_id_request_id 두 제약이 조회된다.
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.table_constraints
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN ('fk_member_current_sanction', 'uk_point_history_member_id_request_id');
