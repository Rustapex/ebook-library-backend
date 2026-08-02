# PR46 감사 로그 DB CHECK 제약 분리 SQL 근거

작성일: 2026-07-14
대상 PR: https://github.com/shangpa/AcornProject/pull/46
대상 후속 항목: 감사 로그 enum의 DB 제약 부재
관련 SQL: `docs/작업현황/2026-07-14_PR46_admin_audit_log_check_constraints.sql`
관련 계획: `docs/작업현황/2026-07-14_PR46_감사로그_DB_CHECK제약_후속계획.md`

---

## 1. 배경

PR #46 최신 리뷰에서는 관리자 API(대시보드/통계/감사로그 등) + 사용자 통계 API 구현이 merge 가능한 상태로 판단됐다.

다만 감사 로그의 `action_type`, `target_type`은 Java enum, Swagger allowableValues, 테스트 레벨에서는 값 동기화가 잡혔지만 DB에서는 `VARCHAR` 컬럼이다. DB CHECK 제약이 없으면 애플리케이션 정상 경로 밖에서 enum 목록에 없는 값이 들어갈 수 있다.

이 문서는 왜 `schema.sql`을 바로 수정하지 않고 별도 SQL 파일로 CHECK 제약을 적용하는지 남기는 근거 문서다.

## 2. DB CHECK가 필요한 이유

Java enum은 애플리케이션 정상 흐름을 방어한다.

- 감사 로그 기록 코드가 enum 타입을 사용하면 enum 외 값을 넘기기 어렵다.
- 감사 로그 조회 필터도 enum 파싱으로 잘못된 요청을 걸러낼 수 있다.
- Swagger enum 동기화 테스트는 API 문서와 Java enum 값의 drift를 잡는다.

하지만 DB 자체는 여전히 `VARCHAR` 값을 받는다. 다음 경로는 Java enum을 거치지 않을 수 있다.

- 수동 SQL INSERT/UPDATE
- 운영 또는 공유 DB 직접 보정
- 외부 배치/import 스크립트
- 향후 다른 코드가 enum을 우회해 문자열을 저장하는 경로
- 기존 데이터 보정 과정에서 잘못된 문자열이 들어가는 경우

따라서 DB CHECK 제약은 애플리케이션 계층 방어와 별개로 DB 무결성을 지키는 마지막 방어선이다.

## 3. `schema.sql`과 분리하는 이유

이번 후속 작업에서는 `src/main/resources/schema.sql`을 수정하지 않는다.

이유는 다음과 같다.

- PR #46 최신 리뷰 기준으로 이 항목은 API 구현 blocker가 아니라 운영/계약 watch 항목이다.
- 이미 만들어진 로컬/공유 DB에는 `schema.sql` 수정만으로 제약이 적용되지 않는다.
- 현재 필요한 것은 기존 DB에 직접 적용 가능한 수동 SQL이다.
- `schema.sql` 수정은 fresh DB 초기화 정책까지 바꾸므로 별도 스키마 변경 범위가 된다.
- 이번 작업은 `idx_reading_progress_last_read_at`처럼 자동 초기화와 분리된 sidecar SQL 적용 정책으로 관리한다.

## 4. 적용 대상

테이블:

- `admin_audit_log`

컬럼:

- `action_type`
- `target_type`

추가 제약명:

- `chk_admin_audit_log_action_type`
- `chk_admin_audit_log_target_type`

허용값 기준:

- `AdminAuditLogActionType` enum
- `AdminAuditLogTargetType` enum

## 5. 적용 절차

1. SQL 파일의 §1 쿼리로 invalid `action_type`, `target_type` 데이터가 있는지 먼저 확인한다.
2. invalid 데이터가 있으면 CHECK 제약 추가 전에 데이터를 정리한다.
3. 같은 이름의 기존 CHECK 제약이 있으면 제거한다.
4. `action_type`, `target_type` CHECK 제약을 추가한다.
5. `information_schema.check_constraints`에서 제약 등록 여부를 확인한다.
6. 로컬/롤백 가능 환경에서만 invalid INSERT 실패 테스트를 선택적으로 수행한다.
7. Swagger/Postman에서 `GET /api/v1/admin/audit-logs?page=0&size=10`을 호출해 기존 조회 API가 정상 동작하는지 확인한다.

## 6. 한계와 후속 결정

이 방식의 한계는 fresh DB에는 CHECK 제약이 자동 반영되지 않는다는 점이다.

현재 결정은 기존/공유 DB에 적용 가능한 분리 SQL을 우선하는 것이다. 추후 모든 fresh DB에도 같은 제약을 자동 생성해야 한다고 결정되면, 그때 `src/main/resources/schema.sql` 반영을 별도 작업으로 잡는다.

## 7. 설계문서 반영 예정 메모

나중에 설계문서 원문을 일괄 반영할 때 아래 내용을 확인한다.

- 감사 로그 테이블의 `action_type`, `target_type`은 Java enum과 DB CHECK 제약의 허용값이 일치해야 한다.
- CHECK 제약은 운영/공유 DB 수동 SQL로 먼저 적용한다.
- `schema.sql` 자동 초기화 반영 여부는 별도 결정 항목으로 유지한다.