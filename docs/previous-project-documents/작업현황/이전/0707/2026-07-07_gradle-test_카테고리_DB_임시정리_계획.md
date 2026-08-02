# 2026-07-07 gradlew test 임시 DB 정리 계획

## 목적

`.\gradlew.bat test` 실패 원인이 코드 문제가 아니라 로컬 MySQL 공유 DB의 기존 `ACTIVE` 카테고리 데이터 때문인지 확인한다.

현재 실패한 테스트는 `CategoryRepositoryTest.findsOnlyActiveCategoriesOrderedByDisplayOrder()`이다. 이 테스트는 실행 중에 `Essay`, `Novel`, `Retired`를 직접 넣고, `ACTIVE` 카테고리 조회 결과가 정확히 `Essay`, `Novel` 두 개만 나오기를 기대한다.

하지만 현재 로컬 DB에는 이미 아래 `ACTIVE` 카테고리가 있어서 테스트 결과에 같이 섞인다.

- `국내도서`
- `컴퓨터/모바일`
- `프로그래밍`

따라서 테스트 실행 전에 기존 `ACTIVE` 카테고리 상태를 백업한 뒤 `INACTIVE`로 임시 변경하고, 테스트가 끝나면 다시 원복한다.

## 주의사항

- 이 SQL은 MySQL Workbench에서 실행한다.
- `category.status`는 `schema.sql` 기준 `ACTIVE`, `INACTIVE`만 허용된다.
- 테스트가 끝나면 반드시 원복 SQL을 실행한다.
- 이 작업은 Swagger 테스트용 데이터에 영향을 줄 수 있으므로, `.\gradlew.bat test` 확인용으로만 짧게 적용한다.
- `TEMPORARY TABLE`이 아니라 일반 백업 테이블을 만든다. Workbench 탭이 바뀌어도 원복할 수 있게 하기 위함이다.

## 1. 테스트 전 현재 상태 확인

먼저 현재 `ACTIVE` 카테고리를 확인한다.

```sql
SELECT category_id, parent_category_id, category_name, display_order, status
FROM category
WHERE status = 'ACTIVE'
ORDER BY display_order ASC, category_id ASC;
```

현재 실패 로그 기준으로는 `국내도서`, `컴퓨터/모바일`, `프로그래밍`이 조회된다.

## 2. 백업 테이블 생성

기존 `ACTIVE` 카테고리 상태를 원복할 수 있게 백업한다.

```sql
CREATE TABLE IF NOT EXISTS zz_test_category_status_backup_20260707 (
    category_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    original_status VARCHAR(20) NOT NULL,
    backed_up_at DATETIME NOT NULL
);
```

이전 실행 흔적이 있으면 먼저 비운다.

```sql
TRUNCATE TABLE zz_test_category_status_backup_20260707;
```

현재 `ACTIVE` 카테고리를 백업한다.

```sql
INSERT INTO zz_test_category_status_backup_20260707 (
    category_id,
    original_status,
    backed_up_at
)
SELECT
    category_id,
    status,
    NOW()
FROM category
WHERE status = 'ACTIVE';
```

백업된 데이터를 확인한다.

```sql
SELECT b.category_id, c.category_name, b.original_status, b.backed_up_at
FROM zz_test_category_status_backup_20260707 b
JOIN category c ON c.category_id = b.category_id
ORDER BY c.display_order ASC, c.category_id ASC;
```

## 3. 기존 ACTIVE 카테고리 임시 비활성화

백업한 카테고리만 `INACTIVE`로 바꾼다.

```sql
UPDATE category c
JOIN zz_test_category_status_backup_20260707 b
  ON b.category_id = c.category_id
SET c.status = 'INACTIVE';
```

변경 후 `ACTIVE` 카테고리가 남아 있는지 확인한다.

```sql
SELECT category_id, parent_category_id, category_name, display_order, status
FROM category
WHERE status = 'ACTIVE'
ORDER BY display_order ASC, category_id ASC;
```

이 쿼리 결과가 비어 있어야 한다.

## 4. Codex에게 테스트 실행 요청

위 SQL을 실행한 뒤 Codex에게 아래 명령을 실행해 달라고 요청한다.

```powershell
.\gradlew.bat test
```

예상 결과:

- `CategoryRepositoryTest`는 통과해야 한다.
- 만약 다른 테스트가 실패하면, 그 실패는 별도 원인으로 다시 확인한다.

## 5. 테스트 후 원복

테스트 성공/실패와 관계없이 반드시 기존 카테고리 상태를 원복한다.

```sql
UPDATE category c
JOIN zz_test_category_status_backup_20260707 b
  ON b.category_id = c.category_id
SET c.status = b.original_status;
```

원복 결과를 확인한다.

```sql
SELECT c.category_id, c.parent_category_id, c.category_name, c.display_order, c.status
FROM category c
JOIN zz_test_category_status_backup_20260707 b
  ON b.category_id = c.category_id
ORDER BY c.display_order ASC, c.category_id ASC;
```

필요하면 백업 테이블을 삭제한다.

```sql
DROP TABLE zz_test_category_status_backup_20260707;
```

## 6. 전체 실행 순서 요약

1. `1. 테스트 전 현재 상태 확인` SQL 실행
2. `2. 백업 테이블 생성` SQL 실행
3. `3. 기존 ACTIVE 카테고리 임시 비활성화` SQL 실행
4. Codex에게 `.\gradlew.bat test` 실행 요청
5. 테스트 결과 확인
6. `5. 테스트 후 원복` SQL 실행
7. Swagger 테스트나 bootRun을 다시 할 예정이면 원복 확인 후 진행

## 판단 기준

- 이 조치 후 `.\gradlew.bat test`가 통과하면, 현재 실패 원인은 코드가 아니라 로컬 공유 DB의 기존 `ACTIVE` 카테고리 데이터였다고 판단한다.
- 이 조치 후에도 다른 테스트가 실패하면, 새 실패 로그를 기준으로 별도 원인을 확인한다.
