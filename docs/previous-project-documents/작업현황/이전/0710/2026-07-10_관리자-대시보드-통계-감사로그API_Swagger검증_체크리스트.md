# 관리자 대시보드 / 통계 관리 / 감사 로그 API — Swagger 검증 체크리스트

작성일: 2026-07-10
관련 계획: `docs/작업현황/2026-07-10_관리자-대시보드-통계-감사로그API_구현계획.md`
대상 코드: `AdminDashboardController`/`AdminStatisticController`/`AdminAuditLogController`, `AdminStatisticBatchService`, `AdminDashboardService`/`AdminStatisticService`/`AdminAuditLogService` — 이미 단위 테스트(158 tests, 0 failures)와 `.\gradlew.bat build` 통과 완료, 이 문서는 그 위에 Swagger로 실제 API 호출까지 확인하는 절차.
사전 준비 SQL: `api-test-results/관리자 API (대시보드통계감사로그 등)/2026-07-10_관리자API_test-env-setup.sql`

---

## 0. 이번에 뭘 테스트하고, 뭘 안 하는지 (범위 명시 — 중요)

**원칙: 이번에 구현한 3개 API(GET 전용)와 그 뒤의 배치만 테스트한다.**

| 대상 | Swagger/Postman 필요? | 비고 |
|---|---|---|
| `GET /api/v1/admin/dashboard` | **필요** | KPI 통계 + 최근 결제 5건 |
| `GET /api/v1/admin/statistics` | **필요** | `periodType`/`ageBand` 검증 포함 |
| `GET /api/v1/admin/audit-logs` | **필요** | 필터(`actionType`/`targetType`/`targetId`) + 페이징 |
| `AdminStatisticBatchService`(배치) | **Swagger 아님 — DB로 확인** | HTTP로 직접 호출하는 API가 아니라 앱 기동 시 1회 + 매일 03:00 실행. §2에서 DB로 확인 |
| 관리자 권한 없는 호출 시 403 | **필요(1회만)** | `/api/v1/admin/**`는 `SecurityConfig`에 이미 있는 공통 규칙이라 새 코드는 아니지만, 이 API에도 실제로 적용되는지는 1회 확인 |
| 감사 로그 **작성**(다른 admin API가 `admin_audit_log`에 실제로 기록하는지) | **불필요, 이번 범위 아님** | 이번 라운드는 조회 API만 구현했고, 어떤 admin API도 아직 `admin_audit_log`에 쓰지 않는다(구현계획 §9 후속 과제). 그래서 §1에서 테스트용 행을 SQL로 직접 넣는다 |
| Postman 동시성 검증(기존 의미 그대로 — 동시 POST/PATCH로 중복 지급/중복 생성 재현) | **불필요, 아예 안 함** | 이번 3개 API는 전부 조회(GET)이고 DB를 바꾸지 않아 동시 요청으로 인한 중복 지급/중복 생성 시나리오 자체가 없음. Swagger UI와 Postman 둘 다 같은 `Authorization: Bearer` 헤더를 그대로 전달할 뿐이라(`SwaggerConfig` 확인 결과), 같은 GET을 두 번 다른 클라이언트로 호출해도 검증 가치가 없어 Postman 자체를 만들지 않기로 함 |
| `AdminStatisticBatchService`의 upsert 동시 실행(같은 시각에 배치가 두 번 겹쳐 돌면 `admin_statistic` UNIQUE 제약 위반 가능) | **알려진 리스크로만 기록, 이번엔 검증 안 함** | 배치는 HTTP 엔드포인트가 아니라 Postman/Swagger 어느 쪽으로도 동시 호출을 재현할 수 없음. 실제로 검증하려면 두 스레드에서 `refreshTotalStatistics()`를 직접 호출하는 동시성 단위 테스트가 필요(후속 작업, 구현계획 §9 후보) |

**결론: Swagger로 확인할 건 3가지 — ①대시보드가 통계+최근결제를 같이 반환하는지 ②통계 API가 `TOTAL`/`ALL` 조합만 되고 나머지는 어떻게 응답하는지(§3-2) ③감사 로그 필터/페이징이 동작하는지. Postman은 만들지 않는다(위 표 참고 — GET 전용이라 검증 가치가 없음).**

---

## 1. 사전 준비

`api-test-results/관리자 API (대시보드통계감사로그 등)/2026-07-10_관리자API_test-env-setup.sql`의 "0. 실행 순서"를 따라 1절~4절을 실행한다(**5절/6절은 §2에서 앱 재기동 후에 실행 — 순서가 중요하다**). 요약:

- 관리자 계정: 기존 `adminuser`(`api-test-results/챌린지출석포인트쿠폰 API/swagger/JWT 회원가입/테스트 데이터 준비.txt` 참고) 재사용. 계정이 없으면 Swagger `POST /api/v1/auth/signup`으로 먼저 만든 뒤 SQL 1절 실행.
- `admin_audit_log`에 테스트용 행 2건을 직접 INSERT(§0에서 설명한 대로 실제 쓰기 경로가 없어서).
- `payment` 테이블에 기존 데이터가 없으면 최소 1건 시드(대시보드 "최근 결제" 확인용).

> **왜 순서가 중요한가**: `AdminStatisticBatchService`는 앱 기동 시 1회만 스냅샷을 찍는다. 배치를 먼저 확인(옛 순서)하고 나중에 결제/대여 데이터를 추가하면, 배치가 그 데이터를 못 본 상태로 남아 `total_rental_count` 같은 값이 실시간 COUNT보다 낮게 나온다 — 실제로 2026-07-11 라운드에서 9(배치) vs 10(실시간) 불일치가 이 순서 문제로 발생했다(계산 버그 아님). 그래서 데이터 시딩(1~4절)을 전부 끝낸 뒤에만 배치를 확인하도록 순서를 바꿨다.

---

## 2. 배치 실행 확인 (Swagger 아님 — 앱 기동 로그 + DB)

1. §1(SQL 1~4절)이 전부 끝난 뒤 앱을 재기동한다(`.\gradlew.bat bootRun --console=plain`).
2. 기동 로그에서 `admin_statistic TOTAL/ALL 통계 갱신 완료` 로그가 찍히는지 확인한다(`AdminStatisticBatchService.refreshOnStartup()`이 `@PostConstruct`로 실행됨).
3. test-env-setup.sql **5절**의 SELECT로 `admin_statistic`에 `period_type='TOTAL', age_band='ALL'` 행이 정확히 1건 있는지, `updated_at`이 방금 재기동한 시각 이후인지 확인한다.
4. **6절** 정합성 확인 쿼리(6개 COUNT/SUM)와 5절 SELECT의 값이 서로 일치하는지 대조한다.

기대: 로그 출력 + 행 1건 + 값 일치. 다르면 어디를 봐야 하는지:
- **행이 아예 없으면** → `@EnableScheduling`(`SchedulingConfig`)이 빠졌거나 `@PostConstruct`가 예외로 실패한 것. 기동 로그에서 스택트레이스 확인.
- **값이 SELECT와 다르면** → `AdminStatisticBatchService`의 count 쿼리(`countByStatus`, `countByBook_RentalType`, `countByCompletedYn`, `sumAmountByPaymentStatus`) 중 하나가 실제 컬럼/CHECK 값과 안 맞는 것.

> 참고(이번엔 검증 안 함): 배치가 같은 순간에 두 번 실행되면(스케줄 실행과 재기동이 겹치는 경우 등) `admin_statistic`의 `UNIQUE(period_type, period_start_date, age_band)` 제약을 두 INSERT가 동시에 위반할 가능성이 이론적으로 있다. 배치는 HTTP 엔드포인트가 아니라 Swagger/Postman 어느 쪽으로도 재현할 수 없어 알려진 리스크로만 남기며, 후속 작업(구현계획 §9)에서 동시성 단위 테스트로 다룬다.

---

## 3. Swagger 실행

### 3-1. `GET /api/v1/admin/dashboard`

```text
GET /api/v1/admin/dashboard
인증: adminuser의 JWT
```

기대 응답: `200 OK`, `data.statistics.periodType="TOTAL"`, `data.statistics.ageBand="ALL"`, `data.recentPayments`가 `paid_at` 내림차순으로 최대 5건.

- `data.statistics.reviewCount`/`reportCount`는 **항상 0**이어야 한다(§0 표에서 설명한 대로 아직 채우지 않음 — 실제로 리뷰/신고가 있어도 0으로 나오는 게 이번 라운드는 정상).
- `data.recentPayments`가 비어 있으면 §1의 payment 시드가 실행됐는지 재확인.

### 3-2. `GET /api/v1/admin/statistics`

```text
GET /api/v1/admin/statistics?periodType=TOTAL
인증: adminuser의 JWT
```

기대: `200 OK`, 대시보드와 같은 통계 값.

아래 케이스도 이어서 확인:

| 요청 | 기대 응답 | 확인 포인트 |
|---|---|---|
| `periodType` 누락 | `400 VALIDATION_ERROR` | `@RequestParam` 필수값 |
| `periodType=DAILY`(허용값 아님) | `400 VALIDATION_ERROR` | `@Pattern` 검증 |
| `periodType=WEEKLY`(허용값이지만 배치가 안 채움) | `404 COMMON_NOT_FOUND` | §4-1 1단계 배치 범위(구현계획 참고) — **500이 아니라 404여야 정상** |
| `periodType=TOTAL&ageBand=10S` | `404 COMMON_NOT_FOUND` | `TOTAL`이어도 `ageBand`가 `ALL`이 아니면 아직 데이터 없음 |
| `periodType=TOTAL&ageBand=TEENAGER`(허용값 아님) | `400 VALIDATION_ERROR` | `ageBand`도 `@Pattern` 검증 대상 |

### 3-3. `GET /api/v1/admin/audit-logs`

```text
GET /api/v1/admin/audit-logs
인증: adminuser의 JWT
```

기대: `200 OK`, §1에서 넣은 테스트 행 2건이 `createdAt` 내림차순(`SWAGGER_TEST_0710_BOOK_CREATE`가 `SWAGGER_TEST_0710_MEMBER_SANCTION`보다 먼저)으로 포함.

추가 케이스:

| 요청 | 기대 응답 |
|---|---|
| `actionType=SWAGGER_TEST_0710_BOOK_CREATE` | 해당 1건만 조회 |
| `targetType=MEMBER` | `SWAGGER_TEST_0710_MEMBER_SANCTION` 1건만 조회 |
| `page=-1` | `400 VALIDATION_ERROR` |
| `size=0` | `400 VALIDATION_ERROR` |

### 3-4. 관리자 권한 없이 호출 (403 확인, 1회만)

일반 회원(`testusera` 등)의 JWT로 위 세 API 중 하나(예: 대시보드)를 호출 → `403 Forbidden` 확인.

---

## 4. 사후 DB 확인

이번 3개 API는 전부 조회 전용이라 호출로 인한 DB 변화가 없어야 한다. 아래로 확인:

```sql
SELECT COUNT(*) AS auditLogCount FROM admin_audit_log WHERE action_type LIKE 'SWAGGER_TEST_0710_%';
-- 2건 그대로 유지(3-3 조회로 늘거나 줄지 않음)

SELECT COUNT(*) AS statCount FROM admin_statistic WHERE period_type = 'TOTAL' AND age_band = 'ALL';
-- 1건 그대로 유지(3-2 조회로 새 행이 생기지 않음)
```

---

## 5. 정리 (테스트 종료 후)

`2026-07-10_관리자API_test-env-setup.sql` §7의 주석 처리된 DELETE 블록을 실행한다(§1절에서 새로 넣은 `admin_audit_log`/`payment`/`book_rental` 행만 대상).

```sql
SELECT COUNT(*) AS remainingCount FROM admin_audit_log WHERE action_type LIKE 'SWAGGER_TEST_0710_%';
-- 0이어야 정상
```

---

## 6. 저장할 증거

```text
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 대시보드/200.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 통계/200.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 통계/WEEKLY 404.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 통계/periodType 허용값 위반 400.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 감사로그/200.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/GET 감사로그/필터 조회.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/일반회원 403.json
api-test-results/관리자 API (대시보드통계감사로그 등)/swagger/앱 기동 로그.png
```

## 7. 최종 체크리스트

| 항목 | 완료 |
|---|---|
| §1 사전 준비 SQL 실행 — 관리자 계정/감사로그 테스트 행/결제 데이터 확보 | [x] |
| §2 배치 실행 확인 — 기동 로그 + `admin_statistic` TOTAL/ALL 1건 + 정합성 값 일치 | [x] (재기동 후 `db 사전.txt`로 확인, 재기동 전 불일치는 `db 오류 난것들.txt`로 남김) |
| §3-1 대시보드 200, `reviewCount`/`reportCount` 0 확인 | [x] |
| §3-2 통계 API — 정상/필수값 누락/허용값 위반/미집계 조합(404) 전부 확인 | [x] (단, `periodType` 누락/`DAILY`는 Swagger UI 클라이언트 단에서 막혀 서버 400 응답 자체는 미저장 — 아래 참고) |
| §3-3 감사 로그 — 목록/필터/페이징 검증(400) 확인 | [x] (`page`/`size` 검증도 §3-2와 동일하게 Swagger 클라이언트 단에서 막힘) |
| §3-4 일반 회원 403 확인 | [x] (3개 API 전부 403 확인, 최소 요구치인 1개보다 많이 함) |
| §4 사후 DB 확인 — 조회로 인한 데이터 변화 없음 | [ ] (별도 증거 없음 — GET 전용이라 위험은 낮으나 미확인 상태) |
| §5 정리 SQL 실행 — 테스트 전용 행 0건 확인 | [ ] (아직 정리 SQL 실행 전으로 보임 — 테스트용 행이 DB에 남아있음) |

## 8. PR 반영 문구 (테스트 완료 후 사용)

```markdown
#### 4. 테스트/검증

- [x] 단위 테스트 확인
- [x] 빌드 확인
- [x] Swagger 실제 API 호출 확인
  - `GET /api/v1/admin/dashboard` → 200, 통계+최근 결제 5건 확인
  - `GET /api/v1/admin/statistics` → 정상(`TOTAL`/`ALL`)/필수값 누락/허용값 위반(400)/미집계 조합(404) 전부 확인
  - `GET /api/v1/admin/audit-logs` → 목록/필터/페이징 확인, 조회로 인한 DB 변화 없음 확인
  - 일반 회원 JWT로 호출 시 403 확인
  - (Postman은 만들지 않음 — 3개 API가 전부 조회 전용이라 별도 클라이언트 교차 확인의 검증 가치가 없다고 판단)
```
