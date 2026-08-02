# 챌린지/출석/포인트/쿠폰 API Swagger 검증 결과 — 코드 수정 계획

작성일: 2026-07-07
상태: **계획 단계 — 코드 미수정**
근거: `api-test-results/챌린지출석포인트쿠폰 API/swagger/**` 실제 응답(json)/DB 캡처(png) 직접 확인 + 현재 로컬 DB 직접 조회(mysql CLI) + `api-test-results/2026-07-07_db_schema_testdata_change_plan.sql`(스키마 유지 정책)

## 0. 결론 요약

Swagger 단건 시나리오 39개 중 실제 파일이 남아있는 걸 기준으로 대부분 실행됐다. 그중 "예상과 다르게 온" 것으로 보고된 항목을 직접 증거(json 응답, DB 캡처, 현재 실DB 조회)로 재검증한 결과:

| 항목 | 판정 | 조치 |
|---|---|---|
| `API-ATTEND-002` yearMonth 형식 오류 → `500` | **실제 코드 버그, 확정** | 아래 1번 |
| `API-CHALLENGE-002` 보상수령 → `grantedPoint:0` | 코드 버그 아님 — **테스트 데이터 누락** + **잠재적 스키마 불일치 버그 발견** | 아래 2번(잠재 버그) + SQL 파일(데이터 보정) |
| `API-ADMIN-CHALLENGE-002` 정상 수정 → "DB 미반영" | **재검증 결과 반증(오탐)** — 현재 DB에 `challenge_reward_id=1, challenge_id=10, reward_point=3500` 정상 존재 | 코드 수정 불필요, 증거만 재캡처 권장 |
| `API-ADMIN-COUPON-002` 정상등록 → "DB 미반영" | **재검증 결과 반증(오탐)** — 현재 DB에 `coupon_id=10` 정상 존재(값 일치) | 코드 수정 불필요, 증거만 재캡처 권장 |

추가로 코드를 다시 읽으며 확인한, Swagger로는 아직 재현 안 됐지만 실제로 유효한 설계 결함 3건(동시성 2건 + 검증 누락 1건)도 이번 계획에 포함한다.

---

## 1. [확정] `GlobalExceptionHandler` 파라미터 바인딩 예외 미처리 → 400 아닌 500

**증거**: `api-test-results/.../API-ATTEND-002 GET/yearMonth 형식 오류/500.json`
```json
{"code": "INTERNAL_SERVER_ERROR", "success": false, "message": "서버 내부 오류가 발생했습니다."}
```

**원인**: `src/main/java/com/acorn/abc/common/exception/GlobalExceptionHandler.java`에 `BusinessException`, `MethodArgumentNotValidException`, catch-all `Exception`(→500)만 있고, `@RequestParam` 바인딩 실패(`MethodArgumentTypeMismatchException`, 필수 파라미터 누락 시 `MissingServletRequestParameterException`)를 처리하는 핸들러가 없다.

**수정 대상**: `GlobalExceptionHandler.java`

**수정 방향**:
```java
@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
public ResponseEntity<ErrorResponse> handleBindingException(Exception e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
}
```
`AttendanceController.getMonthlyAttendance()`의 `yearMonth` 파라미터뿐 아니라, `@RequestParam` 전체(페이지/사이즈 등)에 공통 적용되는 핸들러라 다른 API에도 같이 적용된다.

**이 수정이 기존 코드에 미치는 영향(중요, 검토 완료)**

- `GlobalExceptionHandler`는 `@RestControllerAdvice`로, 프로젝트 전체 `@RestController`에 전역 적용된다(이 파일 하나뿐, 기능별로 나뉘어 있지 않음).
- 이번 수정은 **기존 핸들러 3개(`handleBusinessException`, `handleValidationException`, `handleException`)를 전혀 건드리지 않고, `@ExceptionHandler` 메서드를 1개 새로 추가하는 것뿐**이다. 기존 메서드의 시그니처/로직/순서는 그대로 유지된다.
- Spring은 발생한 예외 타입과 가장 구체적으로 일치하는 핸들러를 고른다. 지금은 `MethodArgumentTypeMismatchException`/`MissingServletRequestParameterException`에 맞는 핸들러가 없어서 catch-all `handleException(Exception e)`(→500)로 떨어지고 있는데, 새 핸들러를 추가하면 이 두 예외만 콕 집어서 먼저 가로챈다. **다른 예외 타입(`BusinessException` 등)의 처리 경로는 전혀 안 바뀐다.**
- **영향 범위**: `@RestControllerAdvice`가 전역이라, 이 두 예외가 발생하는 코드는 내 API뿐 아니라 프로젝트 전체 어디든(다른 팀원의 GET 쿼리 파라미터 검증 등) 이번 수정의 영향을 받는다. 다만 바뀌는 내용은 "그 두 예외 상황에서 응답이 `500 INTERNAL_SERVER_ERROR`에서 `400 VALIDATION_ERROR`로 바뀌는 것"뿐이고, 이건 원래도 500이 의도된 동작이 아니었던 버그 케이스라 사실상 전 프로젝트에 걸친 버그 픽스에 가깝다.
- **기존 테스트 영향 확인**: `src/test/java` 전체에서 `INTERNAL_SERVER_ERROR`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`을 검색한 결과 **일치하는 테스트가 하나도 없음**을 확인했다 — 즉 현재 코드베이스에는 이 변경으로 깨지는 기존 테스트가 없다. 다만 다른 팀원이 로컬에서 작업 중인, 아직 병합 안 된 코드가 "이 두 예외 상황에서 500이 오는 것"을 전제로 무언가를 짜놨다면(가능성은 낮음) 그쪽은 영향받을 수 있으니, 병합 전에 팀 공유는 해두는 게 안전하다.

---

## 2. [확정, 사용자 승인 완료] 챌린지 보상 지급 `pointType` 리터럴이 schema.sql CHECK와 불일치

**배경 정책**(`2026-07-07_db_schema_testdata_change_plan.sql`에서 이미 결정됨): `schema.sql`은 수정하지 않고 유지하며, 코드/테스트 데이터가 `schema.sql`의 허용값에 맞춘다.

**실제 schema.sql 제약(재확인 완료)**:
```sql
-- point_history
CONSTRAINT chk_point_history_point_type CHECK (point_type IN ('challenge_reward', 'REVIEW_REWARD', 'PAYBACK', 'EVENT_REWARD', 'PAYMENT_USE', 'ADMIN_ADJUST'))
-- challenge_reward
CONSTRAINT chk_challenge_reward_reward_type CHECK (reward_type IN ('POINT', 'coupon'))
```
`challenge_reward`(대문자 POINT)와 `point_history`(소문자 challenge_reward)의 대소문자가 서로 다르게 고정돼 있다 — 오타로 보이지만, 이번 정책상 스키마를 고치지 않기로 했으므로 **코드가 이 비대칭을 그대로 따라야 한다.**

**현재 코드**: `challenge/service/ChallengeService.java:79`
```java
pointService.grantOrAdjustPoint(memberId, amount, "CHALLENGE_REWARD", ...);
```
대문자 `"CHALLENGE_REWARD"`를 쓰고 있어 schema의 소문자 `'challenge_reward'`와 불일치한다.

**왜 지금까지 500이 안 났는가**: 이번 Swagger 테스트에서 실제로 사용된 `daily_challenge_id`(challenge_id=9)에 연결된 `challenge_reward` 행이 하나도 없었다(`challenge_reward` 테이블엔 `challenge_id=10`용 1건만 존재). 그래서 `claimReward()`의 포인트 지급 반복문이 아예 실행되지 않아 `point_history` INSERT 자체가 시도되지 않았고, 우연히 버그가 가려졌다(`grantedPoint:0`으로 "성공"만 응답됨). **`challenge_id=9`에 보상을 연결하고 다시 테스트하면 이 INSERT가 실행되어 CHECK 위반으로 500이 날 가능성이 매우 높다.**

**수정 대상**: `challenge/service/ChallengeService.java`

**수정 방향**: 문자열 리터럴을 스키마 허용값에 맞춘다.
```java
pointService.grantOrAdjustPoint(memberId, amount, "challenge_reward", ...);
```
(`ErrorCode`나 `PointHistory` 쪽 로직 변경은 불필요 — `point_type` 컬럼 값만 스키마에 맞추면 됨)

---

## 3. [코드 리뷰에서 확인, 사용자 승인 완료 — Postman 재현 없이 코드 리뷰만으로 수정 진행] 챌린지 보상 수령 동시 요청 시 중복 지급 가능

**대상**: `challenge/service/ChallengeService.java` `claimReward()`

**원인**: `achieved_yn`/`reward_claimed_at` 확인 → 포인트 지급 → `markRewardClaimed()` 순서의 check-then-act 구조라, 동시 요청 2개가 모두 "미수령" 상태를 보고 통과할 수 있다(DB 레벨의 원자적 잠금/조건부 업데이트가 없음).

**수정 방향**: `ChallengeProgress`에 조건부 업데이트를 추가한다. 예를 들어 Repository에 `@Modifying` 쿼리로 `UPDATE challenge_progress SET reward_claimed_at = ? WHERE challenge_progress_id = ? AND reward_claimed_at IS NULL`을 만들어 영향받은 row 수(0 또는 1)로 선점 여부를 판단하고, 0건이면 `CHALLENGE_REWARD_NOT_CLAIMABLE`을 던진 뒤에만 포인트를 지급하도록 순서를 바꾼다(선점 성공 확인 → 지급, 현재는 지급 → 마킹 순서라 반대로 고쳐야 함).

---

## 4. [코드 리뷰에서 확인, 사용자 승인 완료 — Postman 재현 없이 코드 리뷰만으로 수정 진행] 출석 체크 동시 요청 시 409 아닌 500 가능

**대상**: `member/service/AttendanceService.java` `checkIn()`

**원인**: `existsByMember_MemberIdAndAttendanceDate` 확인 후 `save()`하는 구조라, 동시 요청에서는 두 요청 다 "미출석"으로 보고 통과할 수 있다. 이 경우 DB `UNIQUE(member_id, attendance_date)` 위반이 `DataIntegrityViolationException`으로 올라오는데, 이를 잡아서 `ATTENDANCE_ALREADY_EXISTS`로 변환하는 코드가 없어 catch-all 500으로 노출된다.

**수정 방향**: `rental/service/BookmarkService.addBookmark()`에 이미 있는 패턴(`saveAndFlush()` + `catch (DataIntegrityViolationException e) { throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS); }`)을 그대로 적용한다.

---

## 5. [코드 리뷰에서 확인, 확정] 관리자 챌린지 보상 수정 시 `rewardType=POINT`인데 `rewardPoint=null`인 요청을 막지 못함

**대상**: `admin/challenge/dto/AdminChallengeRewardItemRequest.java`, `challenge/service/ChallengeService.java` `updateChallenge()`

**원인**: `rewardType`과 `rewardPoint`/`couponId` 사이의 교차 검증이 없다. `rewardType="POINT"`인데 `rewardPoint=null`이면 `claimReward()`의 `if ("POINT".equals(...) && reward.getRewardPoint() != null)` 조건에서 조용히 스킵되어, 보상 수령은 완료 처리되지만 포인트는 지급되지 않는다.

**수정 방향**: `ChallengeService.updateChallenge()`에서 각 보상 항목 저장 전에 검증을 추가한다.
```java
if ("POINT".equals(item.rewardType()) && item.rewardPoint() == null) {
    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
}
if ("coupon".equals(item.rewardType()) && item.couponId() == null) {
    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
}
```
(2번 항목과 동일하게 `rewardType` 비교값도 스키마 기준 `coupon`은 소문자여야 한다)

**대소문자 근거 확인(`/targeted-file-reading`으로 화면흐름도 4개 문서 전체 확인 완료)**: `01-공통-화면설계기준.md`, `02-User-상세화면설계도.md`, `03-Admin-상세화면설계도.md`, `04-전체-화면이동흐름.md`를 `rewardType`/`reward_type`/`coupon` 키워드로 전수 검색한 결과, **`03-Admin-상세화면설계도.md:352`("보상 | 포인트 또는 쿠폰, 지급 수량")처럼 한글 UI 설명만 있을 뿐, API가 실제로 주고받는 문자열 값의 대소문자 표기를 명시한 곳은 4개 문서 어디에도 없다.** 즉 화면흐름도는 "쿠폰이라는 보상 종류가 있다"는 것만 알려줄 뿐, `rewardType="coupon"`인지 `"COUPON"`인지는 화면설계 문서로 판단할 수 있는 사안이 아니다.

**결론**: 이 값의 유일한 기준은 `schema.sql`의 `chk_challenge_reward_reward_type CHECK (reward_type IN ('POINT', 'coupon'))` 제약뿐이다(2번 항목과 같은 "schema 유지" 정책 적용). 따라서 **`coupon`(소문자)으로 확정**하고, 관리자 프론트가 다른 대소문자로 보낼 가능성은 별도로 프론트 쪽과 맞추는 것과 무관하게 백엔드 검증/저장 값은 스키마 기준을 따른다.

---

## 6. [MEDIUM] `AdminPointAdjustRequest.pointAmount`가 primitive `int`라 필드 누락 시 조용히 0

**대상**: `admin/member/dto/AdminPointAdjustRequest.java`

**수정 방향**: `int` → `Integer`로 바꾸고 `@NotNull` 추가(현재 `description`만 `@NotBlank` 있음). 우선순위 낮음(원치 않는 0원 조정이 실무상 위험하긴 하나, 관리자 전용 API라 즉시성은 낮음).

---

## 7. [재검증 완료, 코드 수정 불필요] "DB 미반영"으로 보고된 2건은 오탐으로 판단

- **`API-ADMIN-CHALLENGE-002` 정상 수정**(challengeId=10): 테스트 당시 캡처한 "사후 DB" 스크린샷은 `challenge_reward_id=NULL`이었지만, 방금 실DB를 직접 조회한 결과 `challenge_reward_id=1, challenge_id=10, reward_type=POINT, reward_point=3500`이 정상적으로 존재한다. 캡처 시점이 실제 커밋 반영 전이었거나 재조회 전 화면을 캡처한 것으로 추정된다.
- **`API-ADMIN-COUPON-002` 정상등록**(couponId=10): 마찬가지로 "사후 DB 확인" 스크린샷은 빈 결과였지만, 실DB엔 `coupon_id=10`이 요청한 값(`10% 할인`, `PERCENT_DISCOUNT`, 10, `PERCENT`, 14일, `ACTIVE`) 그대로 존재한다.
- **결론**: 둘 다 애플리케이션 코드 문제가 아니라 **테스트 중 DB 확인 타이밍 문제**(응답을 받자마자 바로 조회해서 커밋 반영 전 스냅샷을 봤을 가능성, 혹은 같은 Workbench 탭에 이전 결과가 남아있었을 가능성)로 판단된다. 코드 수정 없이 **동일 시나리오를 한 번 더 캡처**해서 최신 결과로 증거 파일만 교체하면 된다.

## 8. [코드 아님, 테스트 데이터 이슈] `challenge_reward` 미연결 + `swagger 테스트용.sql`의 `UNACTIVE` 오타

- `challenge_reward` 테이블에 `daily_challenge_id`(9)용 행이 없다 — 2번 항목의 잠재 버그를 검증하려면 먼저 이 행을 추가해야 한다.
- `api-test-results/swagger 테스트용.sql:39`에 `coupon.status='UNACTIVE'`(오타, `INACTIVE`가 맞음) — 이 INSERT는 CHECK 제약 위반으로 애초에 실패했을 것이다.
- 둘 다 코드가 아니라 테스트 데이터 문제라, 아래 통합 SQL 파일(`2026-07-07_test-env-setup.sql`)에서 정정해서 제공한다.

---

## 9. 남은 결정 필요 사항 — 전부 해소됨 (2026-07-07)

| 항목 | 상태 |
|---|---|
| 2번 수정(`"CHALLENGE_REWARD"` → `"challenge_reward"`) | **확정** — 사용자 승인 완료 |
| 5번 수정에서 `coupon` 비교값 대소문자 | **확정** — 화면흐름도 4개 문서 전수 확인 결과 값 표기 규칙은 문서에 없음, `schema.sql` CHECK 제약 기준으로 `coupon`(소문자) 확정 |
| 3번(동시성, 챌린지 보상) / 4번(동시성, 출석) | **확정** — Postman 실행 불가 상황이라 코드 리뷰만으로 수정 진행 |
| `GlobalExceptionHandler` 수정의 파급 범위 | **검토 완료** — 기존 핸들러 3개는 무수정, 신규 핸들러 1개만 추가(1절 참고). 기존 테스트 중 이 변경으로 깨지는 것 없음(확인 완료) |

더 이상 미해결 결정 사항 없음. 10절 실행 순서대로 진행 가능한 상태.

## 10. 실행 순서(안)

1. `2026-07-07_test-env-setup.sql`로 테스트 데이터 정합성 보정(`challenge_reward` 9번 챌린지 연결 추가, UNACTIVE 오타 있는 잘못된 쿠폰 정리)
2. 1번(`GlobalExceptionHandler`) 수정 — 다른 항목과 독립적이라 먼저 진행 가능
3. 2번(`"CHALLENGE_REWARD"` → `"challenge_reward"`) 수정 후 `API-CHALLENGE-002` #1 재테스트(이번엔 실제 포인트 지급까지 확인)
4. 5번(관리자 챌린지 보상 검증) 수정
5. 3번/4번(동시성) 수정 — Postman 동시 요청 테스트로 검증
6. 6번(`AdminPointAdjustRequest`) 수정
7. 7번 항목 2개는 코드 변경 없이 Swagger 재실행만으로 증거 갱신
8. `.\gradlew.bat build` 통과 확인 후 체크리스트 문서(`2026-07-06_..._테스트시나리오_체크리스트.md`) 최신화
