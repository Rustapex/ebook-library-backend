# PR #50 관리자 컬렉션 API `updatedAt` 수정 계획

대상: PR [#50](https://github.com/shangpa/AcornProject/pull/50) 관리자 컬렉션 API  
작성일: 2026-07-16

## 1. 목적과 범위

`PUT /api/v1/admin/collections/{collectionId}`(API-ADMIN-COLLECTION-002)의 성공 응답에서 DB에 저장된 `updated_at`이 `updatedAt: null`로 반환되는 문제를 보완한다.

- 포함: 수정 서비스의 flush 시점 보장, 응답/DB 시간 일치 통합 테스트, 재검증 체크리스트와 SQL
- 제외: 공개 컬렉션 EVENT 조회 변경, 동시 요청 검증, 비회원 401 응답 변경, ErrorCode·전역 Handler·스키마 변경

## 2. 확인 근거

| 근거 | 확인 내용 | 결론 |
|---|---|---|
| `0717 2차 시도/3-5/200 이어서.json` | API-002 정상 200 응답의 `updatedAt`이 `null` | 응답 생성 시점 결함 후보 |
| `0717 2차 시도/4. DB 사후 확인.txt` | 같은 컬렉션의 `updated_at`은 DB에 저장 | 영속화 자체는 정상 |
| `AdminCollectionService.updateAdminCollection()` | 감사 로그 기록 후 즉시 `AdminCollectionSaveResponse` 생성 | flush 전 Auditing 값 미반영 가능 |
| `BaseEntity.updatedAt` | `@LastModifiedDate` 사용 | flush 시점에 수정 시간이 확정됨 |
| `0717 3차 시도` EVENT 조회 | ACTIVE EVENT 컬렉션이 공개 목록에 반환 | 공개 조회 소스 수정 불필요 |

## 3. 구현 계획

### 3.1 서비스

`AdminCollectionService.updateAdminCollection()`에서 감사 로그 기록 다음, 응답 DTO 생성 전에 `collectionRepository.flush()`를 호출한다.

- 잠금 조회로 관리 상태인 `Collection`의 dirty checking을 유지한다.
- `saveAndFlush()`로 다시 저장하지 않는다. 불필요한 저장 호출 없이 같은 트랜잭션의 변경을 flush한다.
- flush 실패는 기존 `@Transactional` 경계에서 컬렉션 수정 및 감사 로그와 함께 rollback된다.

### 3.2 단위 테스트

기존 Mockito 기반 `AdminCollectionServiceTest`를 확장한다.

1. `CollectionRepository.flush()` 모의 호출에서 Auditing 완료 상태를 재현한다.
2. 반환 `updatedAt`이 모의 Auditing 시간과 같은지 검증한다.
3. 감사 로그 기록 뒤 flush가 호출되는 순서를 검증한다.
4. DB·JDBC·공유 MySQL 데이터는 사용하지 않는다.

## 4. 영향 분석

| 영역 | 영향 | 변경 여부 |
|---|---|---|
| 관리자 컬렉션 수정 API | 성공 응답의 `updatedAt`이 DB 값으로 반환 | 변경 |
| 생성·도서 구성·PATCH API | 호출 흐름 변경 없음 | 미변경 |
| 공개 컬렉션 조회 | 3차 재검증 정상, 변경 없음 | 미변경 |
| ErrorCode·GlobalExceptionHandler | 신규 오류 계약 없음 | 미변경 |
| `schema.sql` | `collection.updated_at`이 이미 존재 | 미변경 |
| 다른 도메인 | 공통 BaseEntity·Handler 변경 없음 | 미영향 |

## 5. 재검증 산출물

- 이해메모: `docs/작업현황/설계문서/이해 메모/2026-07-16_PR50_관리자-컬렉션API_updatedAt_이해메모.md`
- 설계문서 변경 반영예정: `docs/작업현황/설계문서/2026-07-16_PR50_관리자-컬렉션API_updatedAt_설계문서_변경반영예정.md`
- Swagger·DB 체크리스트: `api-test-results/관리자 컬렉션 API/코드 수정 후 재시도/2026-07-16_관리자-컬렉션API_API-002_Swagger-DB_재검증_체크리스트.md`
- 실행·검증·정리 SQL: `api-test-results/관리자 컬렉션 API/코드 수정 후 재시도/2026-07-16_관리자-컬렉션API_API-002_재검증.sql`

Swagger에서 정상 수정이 가능하므로 동일 요청을 Postman으로 중복 수행하지 않는다.

## 6. 검증 순서와 완료 기준

1. `./gradlew.bat test --tests "com.acorn.abc.collection.service.AdminCollectionServiceTest"`
2. `./gradlew.bat build`
3. Swagger API-002 정상 수정 후 HTTP 200 응답의 `data.updatedAt` 비-null 확인
4. SQL 조회 결과의 `updated_at`과 Swagger 응답 시간을 수동 확인
5. 테스트 데이터와 감사 로그를 지정 ID로 정리

완료 기준은 자동 통합 테스트와 build 성공, 그리고 Swagger·DB 수동 검증 증적 기록이다.
