# ABC 데이터베이스 Source of Truth 및 Migration 가이드

> 상태: ACTIVE  
> 최종 검증일: 2026-07-21  
> 검증 환경: MySQL 8.0.40, 실제 `abc` DB 0001/0002 적용 및 전체 테스트 완료

이 문서는 사람과 AI agent가 ABC 데이터베이스를 생성, migration 또는 검증할 때 가장 먼저 읽어야 하는 진입점이다.

## 1. AI agent 필수 규칙

DB 관련 작업을 시작하기 전에 다음 규칙을 반드시 지킨다.

1. 실제 DB에 쓰기 전에 `SELECT VERSION(), DATABASE()`와 `INFORMATION_SCHEMA`로 현재 상태를 확인한다.
2. 기존 DB에는 [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql)을 다시 실행해서 upgrade하려고 하지 않는다. 이 파일은 `CREATE TABLE IF NOT EXISTS` 기반이라 기존 table의 column/index/constraint를 변경하지 않는다.
3. [`docs/데이터베이스/ddl/schema.sql`](ddl/schema.sql)은 과거 논리 DDL 문서이며 runtime column 이름과 다르다. 실행하거나 migration 기준으로 사용하지 않는다.
4. 상태를 확인하지 않은 DB에 migration을 추측으로 실행하지 않는다.
5. MySQL DDL은 implicit commit될 수 있다. 실제 적용 전에 반드시 새 backup을 만들고 복원 가능성을 확인한다.
6. rollback SQL은 사용자가 명시적으로 요청하고, backup이 확보된 경우에만 실행한다.
7. `_migration_*`, `*_backup`, `*_migration_meta` table을 임의로 삭제하지 않는다. rollback 및 감사 근거다.
8. `.env`, DB password, API key를 SQL, 문서, Git diff 또는 로그에 기록하지 않는다.
9. DB 변경 후 `INFORMATION_SCHEMA` 검증, 전체 테스트, 애플리케이션 기동 검증을 모두 수행한다.
10. 애매한 구조가 하나라도 발견되면 DDL을 실행하지 말고 현재 구조와 예상 구조의 차이를 먼저 보고한다.
11. test schema의 table 또는 데이터를 실제 DB에 병합하지 않는다. test schema는 rehearsal과 검증에만 사용한다.

## 2. 현재 상태 증거와 목표 계약

실제 DB가 항상 올바른 목표는 아니다. `INFORMATION_SCHEMA`는 현재 상태를 확인하는 증거이며, 목표 runtime 계약은 versioned SQL과 JPA를 함께 확인해 결정한다.

| 구분 | 기준 | 용도 |
|---|---|---|
| 현재 상태 증거 | 실제 대상 DB의 `INFORMATION_SCHEMA` | 기존 DB가 어떤 상태인지 판정 |
| 신규 DB 목표 | [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql) + 필수 sidecar | 신규 빈 DB bootstrap |
| 기존 DB 목표 | versioned migration + JPA Entity | 현재 상태에서 runtime 계약으로 upgrade |
| 최종 검증 | JPA Entity와 `ddl-auto=validate` | 애플리케이션 호환 검증 |
| 사용 금지 | [`docs/데이터베이스/ddl/schema.sql`](ddl/schema.sql) | 과거 논리 DDL 참고만 가능 |

현재 설정은 다음 두 파일만 Spring Boot가 자동 실행한다.

```properties
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
spring.sql.init.continue-on-error=true
spring.jpa.hibernate.ddl-auto=validate
```

별도 override가 없으면 `schema.sql`과 `data.sql`은 매 기동 시 자동 실행된다. `continue-on-error=true`이므로 SQL 오류가 있어도 초기화가 계속될 수 있다. 애플리케이션이 실행됐다는 사실만으로 모든 index와 constraint가 적용됐다고 판단하지 않는다.

## 3. 최종 runtime 계약

### ISBN 및 도서관

| 대상 | 최종 계약 |
|---|---|
| 도서 ISBN | `book.isbn CHAR(13) NULL` 단일 사용 |
| 제거된 과도기 column | `book.print_isbn13` 없음 |
| 도서관 | `library.library_code`, `region_code`, `dtl_region_code`, `source` 사용 |
| 소장 관계 | `library_holding.isbn` 사용 |
| 소장 UNIQUE | `uk_library_holding (library_id, isbn)` |
| 조회 INDEX | `idx_library_holding_isbn (isbn, library_id)` |
| 구형 관계 | `library_book`은 deprecated지만 backfill 및 호환 목적으로 유지 |

`book_isbn_temp`는 외부 도서 수집 pipeline의 staging table이다. 도서관 조회의 ISBN source가 아니다.

### EPUB 저장 위치 (`book_epub`)

EPUB 원본은 DB LONGBLOB 또는 private S3 object에 저장한다. 최종 계약은 다음과 같다.

| Column | 최종 계약 |
|---|---|
| `epub_data` | `LONGBLOB NULL` |
| `storage_type` | `VARCHAR(16) NOT NULL DEFAULT 'DB'` (`DB` 또는 `S3`) |
| `storage_key` | `VARCHAR(512) NULL`, S3 object key |
| `s3_version_id` | `VARCHAR(128) NULL`, bucket versioning off면 NULL |
| UNIQUE | `uk_book_epub_storage_key (storage_key)` |

허용 상태:

```text
DB row: storage_type='DB', epub_data NOT NULL, storage_key NULL
S3 row: storage_type='S3', storage_key NOT NULL,
        epub_data는 NULL(신규 upload) 또는 rollback용 기존 blob(migration으로 옮긴 row)
```

application(`app.epub.storage=s3`)의 신규 upload row는 항상 `epub_data IS NULL`이다.
one-off migration runner가 옮긴 기존 row는 rollback을 위해 `epub_data`를 유지한다.

row invariant는 V003 postflight, `BookEpubBinaryStore`, `EpubS3MigrationService` preflight가
같은 계약으로 검사한다.

```text
DB row: storage_type='DB', epub_data NOT NULL, storage_key NULL
S3 row: storage_type='S3', storage_key와 s3_version_id 모두 non-blank (NULL과 공백 문자열 모두 위반)
storage_type은 'DB' 또는 'S3'만 허용
```

`s3_version_id`가 필수인 이유: bucket versioning이 배포 필수 조건이고, version 없이 읽으면
"최신 version"을 조용히 돌려주는 fallback이 생기며, 보상 삭제가 key 단위로 동작해
다른 transaction의 object를 지울 수 있기 때문이다.

- `storage_type='S3'`인데 `storage_key`가 비어 있으면 DB blob으로 조용히 fallback하지 않고 실패한다.
- `storage_type`이 `DB`/`S3` 이외의 값이면 DB fallback으로 처리하지 않고 명시적으로 실패한다.
- migration runner는 실행 전 `book_epub` **전체 row**의 invariant를 검사하고, 위반이 있으면
  dry-run에서도 `failed`로 세고 아무것도 옮기지 않은 채 non-zero exit code로 끝난다.

### 외부 인증 계정 연결 (`oauth_account`)

Google 로그인은 변경 가능한 email이 아니라 Google `sub`를 외부 계정 고유 식별자로 사용한다.

| Column | 최종 계약 |
|---|---|
| `oauth_account_id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`, PK |
| `member_id` | `BIGINT UNSIGNED NOT NULL`, FK `fk_oauth_account_member -> member.member_id` |
| `provider` | `VARCHAR(20) NOT NULL` (현재 `GOOGLE`만 허용) |
| `provider_user_id` | `VARCHAR(255) NOT NULL`, Google `sub` |
| `provider_email` | `VARCHAR(255) NULL`, 조회/감사용 snapshot |
| UNIQUE | `uk_oauth_account_provider_provider_user_id (provider, provider_user_id)` |
| UNIQUE | `uk_oauth_account_member_id_provider (member_id, provider)` |
| CHECK | `chk_oauth_account_provider (provider IN ('GOOGLE'))` |

- `provider_email`은 **식별자가 아니다**. 이 column으로 계정을 조회해 자동 연결하지 않는다.
- 기존 회원 연결은 local password 재인증을 거친 명시적 link 경로에서만 생성한다.
- `member` table 구조는 이 기능으로 변경되지 않는다. `login_id`, `password_hash`, `birth_date`는
  NOT NULL을 유지하며 synthetic 값을 만들지 않는다. 신규 Google 사용자는 onboarding에서 실제 값을 입력한다.

### 실제 최종 DB와 bootstrap schema 차이

실제 검증 DB에는 있지만 `src/main/resources/schema.sql`에는 아직 없는 항목이다.

| Table | 실제 DB 추가 항목 | 적용 SQL |
|---|---|---|
| `member` | `current_sanction_id`, FK `fk_member_current_sanction` | `관리자회원_계정정지연결및포인트멱등키.sql` |
| `point_history` | `request_id`, UNIQUE `(member_id, request_id)` | `관리자회원_계정정지연결및포인트멱등키.sql` |
| `point_history` | `balance_after` | `관리자회원_포인트멱등응답스냅샷.sql` |

따라서 신규 DB도 `schema.sql`만 실행해서는 실제 검증 DB와 완전히 같아지지 않는다.

## 4. 작업 전 상태 확인 SQL

다음 query는 read-only다. DB에 쓰기 전에 먼저 실행한다.

```sql
SELECT VERSION() AS mysql_version, DATABASE() AS target_schema;

SELECT
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book'
    ) AS has_book,
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'book' AND COLUMN_NAME = 'isbn'
    ) AS has_book_isbn,
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'book' AND COLUMN_NAME = 'print_isbn13'
    ) AS has_book_print_isbn13,
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'library_holding'
    ) AS has_library_holding,
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'isbn'
    ) AS has_holding_isbn,
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'library_holding' AND COLUMN_NAME = 'print_isbn13'
    ) AS has_holding_print_isbn13,
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'library'
          AND COLUMN_NAME IN ('library_code', 'region_code', 'dtl_region_code', 'source')
    ) AS library_location_column_count;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'book' AND COLUMN_NAME IN
        ('title', 'book_title', 'isbn', 'book_isbn', 'status', 'book_status', 'print_isbn13'))
    OR (TABLE_NAME = 'library' AND COLUMN_NAME IN
        ('status', 'library_status', 'library_code', 'region_code', 'dtl_region_code', 'source'))
    OR (TABLE_NAME = 'library_book' AND COLUMN_NAME IN ('library_id', 'book_id'))
    OR (TABLE_NAME = 'library_holding' AND COLUMN_NAME IN
        ('library_holding_id', 'library_id', 'isbn', 'print_isbn13', 'created_at', 'updated_at'))
    OR (TABLE_NAME = 'member' AND COLUMN_NAME = 'current_sanction_id')
    OR (TABLE_NAME = 'point_history' AND COLUMN_NAME IN ('request_id', 'balance_after'))
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN (
    'idx_book_print_isbn13',
    'uk_library_library_code',
    'idx_library_region_status',
    'uk_library_holding',
    'idx_library_holding_print_isbn13',
    'idx_library_holding_isbn',
    'uk_point_history_member_id_request_id'
  )
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE,
       GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION) AS constraint_columns,
       MAX(kcu.REFERENCED_TABLE_NAME) AS referenced_table,
       GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION) AS referenced_columns
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
  ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
 AND kcu.TABLE_NAME = tc.TABLE_NAME
 AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
  AND (
    (tc.TABLE_NAME = 'library' AND tc.CONSTRAINT_NAME = 'uk_library_library_code')
    OR (tc.TABLE_NAME = 'library_holding' AND tc.CONSTRAINT_NAME IN
        ('PRIMARY', 'uk_library_holding', 'fk_library_holding_library'))
    OR (tc.TABLE_NAME = 'point_history'
        AND tc.CONSTRAINT_NAME = 'uk_point_history_member_id_request_id')
  )
GROUP BY tc.TABLE_NAME, tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;

SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    TABLE_NAME LIKE '\\_migration\\_%'
    OR TABLE_NAME LIKE '%\\_backup'
    OR TABLE_NAME LIKE '%\\_migration_meta'
  )
ORDER BY TABLE_NAME;
```

## 5. DB 상태별 적용 절차

### A. 신규 빈 DB

적용 순서:

1. 빈 schema와 backup 정책을 준비한다.
2. [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql)을 적용한다.
3. 다음 필수 sidecar 2개를 순서대로 적용한다.
   - [`관리자회원_계정정지연결및포인트멱등키.sql`](../../src/main/resources/관리자회원_계정정지연결및포인트멱등키.sql)
   - [`관리자회원_포인트멱등응답스냅샷.sql`](../../src/main/resources/관리자회원_포인트멱등응답스냅샷.sql)
4. `spring.sql.init.mode=always`이면 [`data.sql`](../../src/main/resources/data.sql)이 애플리케이션 기동 때 자동 실행된다는 점을 확인한다. Seed가 필요 없는 환경은 실행 전에 SQL init 설정을 명시적으로 override한다.
5. 애플리케이션을 기동하고 `INFORMATION_SCHEMA`와 `ddl-auto=validate`를 확인한다.
6. 전체 테스트를 실행한다.

신규 빈 DB에서는 다음 기존 DB용 migration을 실행하지 않는다.

- `V001__epub_additive.sql`
- `V002__request_notification_notice_alignment.sql`
- `V003__epub_s3_object_storage.sql`
- `V004__oauth_account.sql`
- `0001_physical_library_location.sql`
- `0002_unify_library_isbn.sql`

이 기능들의 최종 일반 table 구조는 현재 bootstrap schema에 반영돼 있다. 기존 데이터 보정 및 migration tracking table은 신규 DB에 필요하지 않다.

### B. 도서관 기능이 없는 기존 DB

판정 예시:

- `book.isbn`이 `CHAR(13) NULL`이고 `book.title`, `book.status`가 존재
- `book.book_isbn`, `book.book_title`, `book.book_status` 없음
- `book.print_isbn13`, `idx_book_print_isbn13` 없음
- `library.status` 존재, `library.library_status` 없음
- `library_book.library_id`, `library_book.book_id` 존재
- `library.library_code`, `region_code`, `dtl_region_code`, `source`가 모두 없음
- `uk_library_library_code`, `idx_library_region_status` 없음
- `library_holding` 없음
- 위 preflight의 `library_location_column_count = 0`
- `_migration_0001_state`, `_migration_0002_state`, `_migration_0002_book_print_backup`이 모두 없음

위 prerequisite 중 하나라도 다르면 이 절차를 실행하지 않고 `MIXED/UNKNOWN`으로 판정한다.

적용 순서:

1. 신규 backup 및 복원 검증
2. [`0001_physical_library_location.sql`](migration/0001_physical_library_location.sql)
3. 0001 postflight 확인
4. [`0002_unify_library_isbn.sql`](migration/0002_unify_library_isbn.sql)
5. 0002를 한 번 더 실행해 idempotency 확인
6. 최종 구조, 전체 테스트, 애플리케이션 기동 검증

### C. 도서관 0001까지만 적용된 기존 DB

다음 조건을 모두 만족하면 0002만 적용한다.

- `book.isbn`이 `CHAR(13) NULL`
- `book.print_isbn13`이 `CHAR(13) NULL`
- `idx_book_print_isbn13`의 column 순서가 `print_isbn13`
- `library_book.library_id`, `library_book.book_id` 존재
- `library.status` 존재
- `library.library_code`가 `VARCHAR(20) NULL`
- `library.region_code`가 `VARCHAR(2) NULL`
- `library.dtl_region_code`가 `VARCHAR(5) NULL`
- `library.source`가 `VARCHAR(20) NOT NULL DEFAULT 'MANUAL'`
- `library.library_code`에 UNIQUE `uk_library_library_code`가 존재
- `idx_library_region_status`의 column 순서가 `region_code,status,dtl_region_code`
- `library_holding.library_holding_id`가 `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`이고 `PRIMARY KEY`
- `library_holding.library_id`가 `BIGINT UNSIGNED NOT NULL`
- `library_holding.print_isbn13`이 `CHAR(13) NOT NULL`
- `library_holding.created_at`이 `DATETIME NOT NULL`, `updated_at`이 `DATETIME NULL`
- `library_holding.isbn` 없음
- `idx_library_holding_print_isbn13`의 column 순서가 `print_isbn13,library_id`
- `uk_library_holding`의 column 순서가 `library_id,print_isbn13`
- `fk_library_holding_library`가 `library_holding.library_id -> library.library_id`를 참조
- `_migration_0002_state`, `_migration_0002_book_print_backup`이 모두 없음

위 구조를 먼저 확인한 뒤 아래 query 결과가 `0`인지 확인한다. 0이 아니면 0002를 실행하지 않는다.

```sql
SELECT COUNT(*) AS invalid_holding_print_isbn_rows
FROM library_holding
WHERE print_isbn13 IS NULL
   OR print_isbn13 NOT REGEXP '^[0-9]{13}$';
```

위 prerequisite 중 하나라도 다르면 0002를 실행하지 않고 `MIXED/UNKNOWN`으로 판정한다.

적용 파일:

- [`0002_unify_library_isbn.sql`](migration/0002_unify_library_isbn.sql)

Migration tracking table은 판단을 보조하는 감사 근거다. Tracking table 존재 여부만으로 상태를 확정하지 않고 실제 column/index 구조를 우선한다.

### D. 도서관 최종 구조인 기존 DB

다음 조건이면 0001/0002를 다시 실행하지 않는다.

- `book.isbn`이 `CHAR(13) NULL`이고 `book.title`, `book.status`가 존재
- `book.print_isbn13` 없음
- `library.library_code`가 `VARCHAR(20) NULL`
- `library.region_code`가 `VARCHAR(2) NULL`
- `library.dtl_region_code`가 `VARCHAR(5) NULL`
- `library.source`가 `VARCHAR(20) NOT NULL DEFAULT 'MANUAL'`
- `library.library_code`에 UNIQUE `uk_library_library_code`가 존재
- `idx_library_region_status`의 column 순서가 `region_code,status,dtl_region_code`
- `library_holding.library_holding_id`가 `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`이고 `PRIMARY KEY`
- `library_holding.library_id`가 `BIGINT UNSIGNED NOT NULL`
- `library_holding.isbn`이 `CHAR(13) NOT NULL`
- `library_holding.created_at`이 `DATETIME NOT NULL`, `updated_at`이 `DATETIME NULL`
- `library_holding.print_isbn13` 없음
- `idx_library_holding_isbn`의 column 순서가 `isbn,library_id`
- `uk_library_holding`의 column 순서가 `library_id,isbn`
- `fk_library_holding_library`가 `library_holding.library_id -> library.library_id`를 참조

최종 데이터 계약도 확인한다.

```sql
SELECT COUNT(*) AS invalid_holding_isbn_rows
FROM library_holding
WHERE isbn IS NULL
   OR isbn NOT REGEXP '^[0-9]{13}$';
```

결과가 `0`이어야 최종 상태로 판정한다.

### E. `MIXED/UNKNOWN`: 위 조건과 다른 혼합 상태

자동 적용을 중단한다. 다음 내용을 보고한 후 별도 보정 migration을 작성한다.

- 현재 column/index/constraint
- migration tracking table 내용
- 예상 구조와의 차이
- 데이터 row 수와 backup 위치
- 자동 rollback이 안전하지 않은 이유

혼합 상태에서 기존 migration을 억지로 재사용하지 않는다.

## 6. SQL 파일 분류

| 분류 | 파일 | 적용 대상 |
|---|---|---|
| 자동 bootstrap | `src/main/resources/schema.sql` | 신규 빈 DB |
| seed | `src/main/resources/data.sql` | 필요한 신규/개발 DB만 |
| 필수 sidecar | `관리자회원_계정정지연결및포인트멱등키.sql` | 신규 및 해당 column이 없는 기존 DB |
| 필수 sidecar | `관리자회원_포인트멱등응답스냅샷.sql` | 신규 및 해당 column이 없는 기존 DB |
| 기존 DB migration | `db/manual/V001__epub_additive.sql` | EPUB 구조가 없는 기존 DB만 |
| 기존 DB migration | `db/manual/V002__request_notification_notice_alignment.sql` | 희망도서/알림/공지 정합화가 안 된 기존 DB만 |
| 기존 DB migration | `db/manual/V003__epub_s3_object_storage.sql` | `book_epub`에 S3 metadata column이 없는 기존 DB만 |
| 기존 DB migration | `db/manual/V004__oauth_account.sql` | `oauth_account`가 없는 기존 DB만 (Google 로그인) |
| 도서관 migration | `migration/0001_physical_library_location.sql` | 도서관 기능이 없는 기존 DB |
| 도서관 migration | `migration/0002_unify_library_isbn.sql` | 0001 ISBN 구조인 기존 DB |
| 선택 성능 보완 | `관리자통계_성능인덱스.sql` | index 부재 확인 후 선택 적용 |
| 선택 DB guard | `관리자감사로그_CHECK제약.sql` | 기존 invalid 데이터 0건 확인 후 선택 적용 |
| rollback | `U001`, `U002`, `*_rollback.sql` | 명시 승인과 backup이 있을 때만 |

V001/U001, V002/U002, V003은 [`src/main/resources/db/manual`](../../src/main/resources/db/manual)에 있다.
V003에는 대응하는 자동 rollback SQL(U003)이 없다. 되돌리려면 적용 직전 backup/snapshot을 복원한다.

### V003 실행 규칙

- `DELIMITER`를 사용하는 stored procedure 기반이므로 **반드시 `mysql` CLI로 실행**한다.
- 실행 후 반드시 exit code를 확인한다. `0`이 아니면 구조가 기대와 다른 것이므로 추측으로 고치지 않는다.
- V003은 기존 column의 정확한 type/nullability/default, unique index 대상 column, row invariant를
  모두 검사하고, 어긋나면 `SIGNAL SQLSTATE '45000'`으로 **DDL을 하나도 적용하지 않고** 중단한다.
- 신규 column이 3개 중 1~2개만 있는 부분 적용 상태는 `MIXED`로 판정하고 실패한다.
- postflight도 procedure 안에서 SIGNAL을 던지므로 기대 구조가 아니면 exit 0이 나오지 않는다.
- 자세한 실패 메시지 표는 [`deploy/README.md`](../../deploy/README.md) §2-3에 있다.

### V004 실행 규칙

Google 로그인용 `oauth_account`를 기존 DB에 추가하는 additive migration이다.

**분류와 대상**

- 기존 DB 전용이다. 신규 빈 DB에는 실행하지 않는다(§5-A). 최종 구조는 이미 `src/main/resources/schema.sql`에 있고,
  `deploy/db/bootstrap-fresh.sh`가 fresh bootstrap manifest에서 `V00*.sql`을 거부한다.
- `docs/데이터베이스/ddl/schema.sql`은 legacy이므로 이 변경을 반영하지 않으며 실행하지도 않는다.

**상태 판별 (실행 전)**

```sql
SELECT
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oauth_account'
    ) AS has_oauth_account,
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oauth_account'
    ) AS oauth_account_column_count;
```

| 결과 | 판정 | 조치 |
|---|---|---|
| `has_oauth_account = 0` | 미적용 | V004 실행 |
| `has_oauth_account = 1`, column 7개가 최종 계약과 일치 | 적용 완료 | 재실행해도 무해(멱등). 실행 생략 가능 |
| `has_oauth_account = 1`, 구조가 다르거나 부분 적용 | `MIXED` | **실행하지 않는다.** V004가 SIGNAL로 중단하며, 차이를 먼저 보고한다 |

**실행**

- `DELIMITER`와 stored procedure를 쓰므로 **반드시 `mysql` CLI로 실행**한다(gradle sql-init/JDBC multi-statement 금지).
- 실행 후 exit code를 확인한다. `0`이 아니면 구조가 기대와 다른 것이므로 추측으로 고치지 않는다.
- 부분 적용 상태는 preflight에서 `MIXED`로 판정하고 **DDL을 하나도 적용하지 않은 채** 실패한다.

```bash
mysql -h <host> -u <user> -p <database> < src/main/resources/db/manual/V004__oauth_account.sql
```

**멱등성**

2회 이상 실행해도 결과가 같다. 이미 최종 구조면 preflight가 구조 일치를 확인하고 아무것도 바꾸지 않는다.
FK는 `INFORMATION_SCHEMA` 확인 후 없을 때만 추가한다(MySQL DDL의 implicit commit 때문).

**Postflight**

procedure 안에서 다음을 단언하고, 어긋나면 `SIGNAL SQLSTATE '45000'`으로 exit code 0을 내지 않는다.

- column 7개의 이름/type/nullability 일치, 계약 밖 column 없음
- `uk_oauth_account_provider_provider_user_id (provider, provider_user_id)`
- `uk_oauth_account_member_id_provider (member_id, provider)`
- `fk_oauth_account_member -> member`
- `chk_oauth_account_provider`

CI(`deploy/db/verify-schema-contract.sh`)는 여기에 더해 `base + V004` 결과가 head canonical fresh bootstrap과
수렴하는지, 2회 실행이 멱등한지를 자동 검증한다. 따라서 runtime contract는 v1을 유지한다
(v1이 단언하는 `book`/`book_epub`/`library`/`library_holding`/`member`/`point_history` 계약은 이 변경으로 바뀌지 않는다).

**Rollback**

- `oauth_account`는 additive다. backend image rollback 시 **즉시 drop하지 않는다.**
  구버전 image는 이 table을 참조하지 않으므로 그대로 두어도 기존 로그인과 `ddl-auto=validate`가 깨지지 않는다.
- 기능만 되돌릴 때는 DB가 아니라 `GOOGLE_AUTH_ENABLED=false`로 먼저 차단한다.
- 구조 rollback(`DROP TABLE oauth_account`)은 명시 승인과 backup이 있을 때만 수행한다.
  이미 Google 전용 회원이 생성된 뒤라면 그 회원의 연결 정보가 소실되므로 data-loss 판단이 선행돼야 한다.
- 대응하는 자동 rollback SQL(U004)은 만들지 않는다.

## 7. 실제 검증 DB 상태 기록

2026-07-21 검증 기준:

- MySQL 8.0.40
- 도서관 0001 및 0002 적용 완료
- `book.isbn` 단일 ISBN 사용
- `library_holding.isbn` 최종 구조
- 0002 2회 실행 idempotency 확인
- 전체 테스트 688개, failure 0, error 0, skipped 1
- 애플리케이션 기동 및 정보나루 실데이터 smoke test 통과
- 관리자 회원 필수 sidecar 2개 적용 상태
- EPUB V001 및 희망도서/알림/공지 V002 구조 적용 상태
- 관리자 통계 성능 index는 적용되지 않은 상태
- 관리자 감사 로그 CHECK 제약은 적용되지 않은 상태

실제 검증 DB에는 runtime 일반 table 외에 migration/rollback 추적용 table이 남아 있다. 이 table들은 신규 빈 DB bootstrap에는 필요하지 않지만 기존 DB에서는 삭제하지 않는다.

## 8. 변경 완료 조건

AI agent는 다음 조건을 모두 증명하기 전 완료라고 보고하지 않는다.

- 대상 schema와 MySQL version 확인
- 적용 전 backup 및 복원 가능성 확인
- migration exit code 0
- 필요한 경우 2회 실행 idempotency 확인
- `INFORMATION_SCHEMA` column/index/constraint 검증
- 기존 주요 table row 수 불변 확인
- 전체 테스트 통과
- 애플리케이션 `ddl-auto=validate` 통과
- smoke test 통과
- password, API key, 외부 API URL이 로그에 노출되지 않음
- 임시 process 종료 및 변경한 server 설정 원복

## 9. 향후 DB 변경 규칙

새 DB 변경을 추가할 때는 다음 세 가지를 같은 PR에서 처리한다.

1. 신규 빈 DB용 `src/main/resources/schema.sql` 갱신
2. 기존 DB용 idempotent migration 또는 sidecar 추가
3. 이 문서의 SQL 분류, 상태 분기, 최종 계약 갱신

수동 SQL만 추가하고 bootstrap schema와 이 문서를 갱신하지 않는 방식을 반복하지 않는다.
