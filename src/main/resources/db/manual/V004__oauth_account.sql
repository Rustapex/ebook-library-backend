-- Google 로그인 구현: 기존 DB에 oauth_account를 additive로 추가하는 migration.
-- 관련 계획: .omc/plans/google-login-redeploy-release-plan.md Phase 2
--
-- 성격: additive. 기존 table의 column/제약/데이터를 변경하지 않는다.
--   - 새 table oauth_account 1개 생성
--   - member table은 읽기 전용 참조(FK)만 추가하고 구조를 바꾸지 않는다
--   - 기존 회원의 로그인 경로에 영향이 없다
--
-- 신규 빈 DB에는 실행하지 않는다. 최종 구조는 이미 src/main/resources/schema.sql에 반영돼 있고,
-- deploy/db/bootstrap-fresh.sh가 V00*.sql을 fresh bootstrap에서 명시적으로 거부한다.
-- (docs/데이터베이스/README.md §5-A, §6)
--
-- 실행 방법: DELIMITER와 stored procedure를 사용하므로 반드시 mysql CLI로 실행한다.
--   mysql -h <host> -u <user> -p <database> < V004__oauth_account.sql
-- 실행 후 exit code를 반드시 확인한다. 0이 아니면 구조가 기대와 다른 것이므로 추측으로 고치지 않는다.
--
-- 멱등성: 2회 이상 실행해도 결과가 같다. 이미 최종 구조면 아무것도 바꾸지 않고 통과한다.
-- MIXED 판정: oauth_account가 이미 있으나 구조가 최종 계약과 다르면 DDL을 하나도 적용하지 않고
--            SIGNAL SQLSTATE '45000'으로 중단한다. (docs/데이터베이스/README.md §5-E)
--
-- rollback: oauth_account는 additive이므로 backend rollback 시 즉시 제거하지 않는다.
--           구조 rollback은 명시 승인과 snapshot이 있을 때만 별도로 수행한다.

-- ============================================================
-- 0. 대상 DB 확인 (적용 증거용)
-- ============================================================
SELECT DATABASE() AS current_database, VERSION() AS mysql_version;

-- ============================================================
-- 1. Preflight + 적용
-- ============================================================
DROP PROCEDURE IF EXISTS sp_abc_v004_oauth_account_apply;
DELIMITER $$
CREATE PROCEDURE sp_abc_v004_oauth_account_apply()
BEGIN
  DECLARE member_column_ok INT;
  DECLARE oauth_table_exists INT;
  DECLARE oauth_column_ok INT;
  DECLARE oauth_unique_ok INT;
  DECLARE oauth_foreign_key_exists INT;

  -- 1-1. ABC runtime schema인지 확인한다. member.member_id가 없으면 대상 DB가 아니다.
  SELECT COUNT(*) INTO member_column_ok
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member'
    AND COLUMN_NAME = 'member_id'
    AND DATA_TYPE = 'bigint'
    AND COLUMN_TYPE LIKE '%unsigned%';

  IF member_column_ok <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 preflight failed: member.member_id BIGINT UNSIGNED not found. Target is not an ABC runtime schema.';
  END IF;

  -- 1-2. oauth_account 현재 상태를 판별한다.
  SELECT COUNT(*) INTO oauth_table_exists
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oauth_account';

  IF oauth_table_exists > 0 THEN
    -- 이미 존재하면 최종 계약과 완전히 같을 때만 재실행을 허용한다(멱등 경로).
    -- column이 하나라도 다르거나 빠져 있으면 부분 적용 상태이므로 MIXED로 판정한다.
    SELECT COUNT(*) INTO oauth_column_ok
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oauth_account'
      AND (
        (COLUMN_NAME = 'oauth_account_id' AND DATA_TYPE = 'bigint'
          AND COLUMN_TYPE LIKE '%unsigned%' AND IS_NULLABLE = 'NO' AND EXTRA LIKE '%auto_increment%')
        OR (COLUMN_NAME = 'member_id' AND DATA_TYPE = 'bigint'
          AND COLUMN_TYPE LIKE '%unsigned%' AND IS_NULLABLE = 'NO')
        OR (COLUMN_NAME = 'provider' AND DATA_TYPE = 'varchar'
          AND CHARACTER_MAXIMUM_LENGTH = 20 AND IS_NULLABLE = 'NO')
        OR (COLUMN_NAME = 'provider_user_id' AND DATA_TYPE = 'varchar'
          AND CHARACTER_MAXIMUM_LENGTH = 255 AND IS_NULLABLE = 'NO')
        OR (COLUMN_NAME = 'provider_email' AND DATA_TYPE = 'varchar'
          AND CHARACTER_MAXIMUM_LENGTH = 255 AND IS_NULLABLE = 'YES')
        OR (COLUMN_NAME = 'created_at' AND DATA_TYPE = 'datetime' AND IS_NULLABLE = 'NO')
        OR (COLUMN_NAME = 'updated_at' AND DATA_TYPE = 'datetime' AND IS_NULLABLE = 'YES')
      );

    IF oauth_column_ok <> 7 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V004 preflight failed: oauth_account exists with an unexpected column contract (MIXED). Inspect INFORMATION_SCHEMA before applying anything.';
    END IF;

    -- 외부 계정 식별의 핵심 제약이다. 둘 중 하나라도 없으면 중복 연결을 막을 수 없다.
    SELECT COUNT(*) INTO oauth_unique_ok
    FROM (
      SELECT INDEX_NAME
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'oauth_account'
        AND INDEX_NAME IN (
          'uk_oauth_account_provider_provider_user_id',
          'uk_oauth_account_member_id_provider'
        )
        AND NON_UNIQUE = 0
      GROUP BY INDEX_NAME, NON_UNIQUE
      HAVING (INDEX_NAME = 'uk_oauth_account_provider_provider_user_id'
               AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'provider,provider_user_id')
          OR (INDEX_NAME = 'uk_oauth_account_member_id_provider'
               AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'member_id,provider')
    ) existing_unique_indexes;

    IF oauth_unique_ok <> 2 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V004 preflight failed: oauth_account exists without both required UNIQUE constraints (MIXED).';
    END IF;
  ELSE
    -- 최종 계약. src/main/resources/schema.sql의 oauth_account 정의와 동일해야 한다.
    -- (CI verify-schema-contract.sh가 base+V004 결과와 head fresh bootstrap의 수렴을 검증한다)
    CREATE TABLE oauth_account (
      oauth_account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
      member_id BIGINT UNSIGNED NOT NULL,
      provider VARCHAR(20) NOT NULL,
      provider_user_id VARCHAR(255) NOT NULL,
      provider_email VARCHAR(255) NULL,
      created_at DATETIME NOT NULL,
      updated_at DATETIME NULL,
      CONSTRAINT pk_oauth_account PRIMARY KEY (oauth_account_id),
      CONSTRAINT uk_oauth_account_provider_provider_user_id UNIQUE (provider, provider_user_id),
      CONSTRAINT uk_oauth_account_member_id_provider UNIQUE (member_id, provider),
      CONSTRAINT chk_oauth_account_provider CHECK (provider IN ('GOOGLE'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
  END IF;

  -- 1-3. FK를 멱등하게 추가한다. MySQL DDL은 implicit commit되므로 존재 여부를 먼저 확인한다.
  SELECT COUNT(*) INTO oauth_foreign_key_exists
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oauth_account'
    AND CONSTRAINT_NAME = 'fk_oauth_account_member'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY';

  IF oauth_foreign_key_exists = 0 THEN
    ALTER TABLE oauth_account
      ADD CONSTRAINT fk_oauth_account_member
      FOREIGN KEY (member_id) REFERENCES member(member_id);
  END IF;
END$$
DELIMITER ;

CALL sp_abc_v004_oauth_account_apply();

-- ============================================================
-- 2. Postflight: 최종 구조가 아니면 exit code 0을 내지 않는다
-- ============================================================
DROP PROCEDURE IF EXISTS sp_abc_v004_oauth_account_postflight;
DELIMITER $$
CREATE PROCEDURE sp_abc_v004_oauth_account_postflight()
BEGIN
  DECLARE final_column_count INT;
  DECLARE final_total_column_count INT;
  DECLARE final_unique_count INT;
  DECLARE final_foreign_key_count INT;
  DECLARE final_check_count INT;

  -- column 개수만 세면 type이 어긋난 상태를 통과시킨다. 이름과 type/nullability를 함께 단언한다.
  SELECT COUNT(*) INTO final_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oauth_account'
    AND (
      (COLUMN_NAME = 'oauth_account_id' AND DATA_TYPE = 'bigint'
        AND COLUMN_TYPE LIKE '%unsigned%' AND IS_NULLABLE = 'NO' AND EXTRA LIKE '%auto_increment%')
      OR (COLUMN_NAME = 'member_id' AND DATA_TYPE = 'bigint'
        AND COLUMN_TYPE LIKE '%unsigned%' AND IS_NULLABLE = 'NO')
      OR (COLUMN_NAME = 'provider' AND DATA_TYPE = 'varchar'
        AND CHARACTER_MAXIMUM_LENGTH = 20 AND IS_NULLABLE = 'NO')
      OR (COLUMN_NAME = 'provider_user_id' AND DATA_TYPE = 'varchar'
        AND CHARACTER_MAXIMUM_LENGTH = 255 AND IS_NULLABLE = 'NO')
      OR (COLUMN_NAME = 'provider_email' AND DATA_TYPE = 'varchar'
        AND CHARACTER_MAXIMUM_LENGTH = 255 AND IS_NULLABLE = 'YES')
      OR (COLUMN_NAME = 'created_at' AND DATA_TYPE = 'datetime' AND IS_NULLABLE = 'NO')
      OR (COLUMN_NAME = 'updated_at' AND DATA_TYPE = 'datetime' AND IS_NULLABLE = 'YES')
    );

  IF final_column_count <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 postflight failed: oauth_account column contract mismatch (expected 7 matching columns).';
  END IF;

  -- 계약 밖 column이 추가로 붙은 상태도 최종 구조가 아니다.
  SELECT COUNT(*) INTO final_total_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oauth_account';

  IF final_total_column_count <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 postflight failed: oauth_account has unexpected extra columns.';
  END IF;

  SELECT COUNT(*) INTO final_unique_count
  FROM (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oauth_account'
      AND NON_UNIQUE = 0
      AND INDEX_NAME IN (
        'uk_oauth_account_provider_provider_user_id',
        'uk_oauth_account_member_id_provider'
      )
    GROUP BY INDEX_NAME
    HAVING (INDEX_NAME = 'uk_oauth_account_provider_provider_user_id'
             AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'provider,provider_user_id')
        OR (INDEX_NAME = 'uk_oauth_account_member_id_provider'
             AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'member_id,provider')
  ) final_unique_indexes;

  IF final_unique_count <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 postflight failed: required UNIQUE constraints are missing.';
  END IF;

  SELECT COUNT(*) INTO final_foreign_key_count
  FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oauth_account'
    AND CONSTRAINT_NAME = 'fk_oauth_account_member'
    AND REFERENCED_TABLE_NAME = 'member';

  IF final_foreign_key_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 postflight failed: fk_oauth_account_member is missing.';
  END IF;

  SELECT COUNT(*) INTO final_check_count
  FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'chk_oauth_account_provider';

  IF final_check_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V004 postflight failed: chk_oauth_account_provider is missing.';
  END IF;
END$$
DELIMITER ;

CALL sp_abc_v004_oauth_account_postflight();

-- ============================================================
-- 3. 일회성 routine 정리
-- ============================================================
DROP PROCEDURE IF EXISTS sp_abc_v004_oauth_account_apply;
DROP PROCEDURE IF EXISTS sp_abc_v004_oauth_account_postflight;

-- ============================================================
-- 4. 적용 증거용 최종 구조 출력
-- ============================================================
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oauth_account'
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oauth_account'
GROUP BY INDEX_NAME, NON_UNIQUE
ORDER BY INDEX_NAME;

SELECT COUNT(*) AS oauth_account_row_count FROM oauth_account;
