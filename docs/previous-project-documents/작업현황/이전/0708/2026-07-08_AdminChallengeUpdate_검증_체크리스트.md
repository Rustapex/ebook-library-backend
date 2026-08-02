# AdminChallengeUpdateRequest.challengeType/status 허용값 검증 — Swagger 테스트 체크리스트

작성일: 2026-07-08 (2차 개정 — 백업/전체비움 방식에서 "기존 데이터 유지 + 신규 행만 추가" 방식으로 변경)
관련 문서: `docs/작업현황/2026-07-08_PR33_3차코드리뷰_및_테스트격리_대응계획.md` §1, §4
관련 SQL: `api-test-results/챌린지출석포인트쿠폰 API 0708/사전 확인.sql`
대상 코드(수정 예정, 아직 미적용): `AdminChallengeUpdateRequest.java`의 `challengeType`/`status`에 `@Pattern` 추가

**방식 변경 이력**: 처음엔 `challenge`/`challenge_reward` 테이블을 통째로 백업 → 비움 → 원복하는 방식으로 진행했으나, `challenge_progress`가 `challenge_id`에 FK(`fk_challenge_progress_challenge`, `schema.sql:634`)를 걸고 있어 `DELETE FROM challenge`가 "Cannot delete or update a parent row" 오류로 실패했다. `challenge_progress`까지 백업/원복 범위에 넣으면 이번 세션에서 쌓아온 실제 진행도 데이터까지 건드리는 위험이 커져서, **기존 데이터는 전혀 건드리지 않고 테스트 전용 새 행만 추가하는 방식**으로 바꿨다(테이블을 비우지 않으므로 FK 문제 자체가 생기지 않는다).

이 문서와 SQL 파일은 같은 절 번호(`§0`~`§4`)를 서로 그대로 참조한다. **지금 SQL 파일에서 몇 번째 절까지 실행했는지 먼저 확인한 뒤**, 이 문서에서 같은 번호가 붙은 안내를 찾아 다음 순서로 진행한다. 반대로 이 문서를 보다가 "SQL §N 실행" 안내가 나오면 SQL 파일을 열어 그 절만 실행하고 돌아온다.

## 0. 이 체크리스트를 쓰기 전에 (순서대로)

| 순서 | 할 일 | 완료 |
|---|---|---|
| 1 | 코드 수정(`AdminChallengeUpdateRequest`에 `@Pattern` 추가)이 적용되고 서버가 그 코드로 **재시작**돼 있는지 확인. 서버를 재시작하지 않으면 예전 코드 그대로 500이 나올 수 있다(이전 라운드에서 실제로 겪은 문제) | [ ] |
| 2 | 이전에 "백업/전체비움" 방식으로 시도하다가 중간에 멈췄다면, SQL 파일 맨 위 "⚠ 지금 막힌 상태를 먼저 복구해야 한다면" 블록부터 실행해서 `challenge_reward`를 원복하고 백업 테이블을 지운다 | [ ] |
| 3 | SQL 파일 **§0** 실행(계정 ADMIN 승격 + `@admin_id` 세션 변수) — `adminId`가 `NULL`이 아닌지 확인 | [ ] |
| 4 | SQL 파일 **§1** 실행(테스트 전용 챌린지 + 보상 새로 추가, 기존 데이터는 안 건드림) — `testChallengeId`/`testChallengeName` 값을 확인하고 아래 `{testChallengeId}`에 그 값을 쓴다 | [ ] |
| 5 | 관리자 계정은 `adminuser`(`api-test-results/챌린지출석포인트쿠폰 API/swagger/JWT 회원가입/테스트 데이터 준비.txt`에 있는, 이미 Swagger로 회원가입 완료된 계정)로 로그인해 JWT 확보 | [ ] |

**주의**: SQL §1까지 실행하지 않고(=테스트 전용 데이터가 없는 상태에서) 원래 있던 실제 챌린지로 이 테스트를 진행하면 안 된다(다른 데이터를 건드리게 됨).

---

## 3-1. API-ADMIN-CHALLENGE-002 — 챌린지 수정 (`challengeType`/`status` 허용값 검증)

> **시작 전 확인**: 위 "0."의 1~5번이 전부 체크됐는지, 즉 SQL 파일 **§1까지 실행 완료**했는지 확인 후 아래로 진행한다.

사전 DB 확인(SQL §1에서 이미 확인했지만 Swagger 테스트 직전에 한 번 더):

```sql
SELECT c.challenge_id, c.challenge_name, c.challenge_type, c.target_action, c.target_count, c.status,
       cr.challenge_reward_id, cr.reward_type, cr.reward_point, cr.reward_quantity
FROM challenge c
LEFT JOIN challenge_reward cr ON cr.challenge_id = c.challenge_id
WHERE c.challenge_id = @test_challenge_id;
```

### #1 challengeType 허용값 위반

Swagger 요청:

```text
PUT /api/v1/admin/challenges/{testChallengeId}
인증: 관리자(adminuser)
body:
{
  "challengeName": "SWAGGER_TEST_0708_챌린지검증용",
  "challengeType": "WEEKLY",
  "targetAction": "ATTENDANCE",
  "targetCount": 1,
  "status": "ACTIVE",
  "rewards": [
    {
      "rewardType": "POINT",
      "rewardPoint": 100,
      "couponId": null,
      "rewardQuantity": 1
    }
  ]
}
```

`challengeType`을 스키마 CHECK 제약(`DAILY`/`TOTAL`)에 없는 `"WEEKLY"`로 넣는다.

기대:

- 정상 기대는 `400 VALIDATION_ERROR`
- 실제로 다르게 나오면 어디를 봐야 하는지:
  - **`500 INTERNAL_SERVER_ERROR`가 나오면** → `AdminChallengeUpdateRequest.challengeType`에 `@Pattern`이 실제로 반영됐는지, 서버가 최신 코드로 재시작됐는지부터 확인(위 "0." 1번 참고). `@Pattern`이 없으면 DB CHECK 제약까지 통과해버려 flush 시점에 `DataIntegrityViolationException`이 나고, 이걸 처리하는 핸들러가 `GlobalExceptionHandler`에 없어 500이 된다.
  - **`200 OK`가 나오면** → 검증 자체가 아예 걸리지 않은 것. `@Pattern`의 `regexp` 값이나 어노테이션 위치를 다시 확인.

사후 DB 확인(값이 바뀌지 않았어야 정상 — SQL 실행은 이 SELECT 하나면 충분하고 §2/§4 재실행은 필요 없다):

```sql
SELECT challenge_id, challenge_type, status
FROM challenge
WHERE challenge_id = @test_challenge_id;
```

### #2 status 허용값 위반

Swagger 요청:

```text
PUT /api/v1/admin/challenges/{testChallengeId}
인증: 관리자(adminuser)
body:
{
  "challengeName": "SWAGGER_TEST_0708_챌린지검증용",
  "challengeType": "DAILY",
  "targetAction": "ATTENDANCE",
  "targetCount": 1,
  "status": "DELETED",
  "rewards": [
    {
      "rewardType": "POINT",
      "rewardPoint": 100,
      "couponId": null,
      "rewardQuantity": 1
    }
  ]
}
```

`status`를 스키마 CHECK 제약(`ACTIVE`/`INACTIVE`)에 없는 `"DELETED"`로 넣는다.

기대:

- 정상 기대는 `400 VALIDATION_ERROR`
- 실제로 다르게 나오면 어디를 봐야 하는지: #1과 동일 — 500이면 `@Pattern` 미반영/서버 미재시작 의심, 200이면 `regexp`/어노테이션 위치 재확인.

사후 DB 확인(값이 바뀌지 않았어야 정상):

```sql
SELECT challenge_id, challenge_type, status
FROM challenge
WHERE challenge_id = @test_challenge_id;
```

### #3 정상값 회귀 확인

Swagger 요청:

```text
PUT /api/v1/admin/challenges/{testChallengeId}
인증: 관리자(adminuser)
body:
{
  "challengeName": "SWAGGER_TEST_0708_챌린지검증용_수정",
  "challengeType": "TOTAL",
  "targetAction": "ATTENDANCE",
  "targetCount": 10,
  "status": "INACTIVE",
  "rewards": [
    {
      "rewardType": "POINT",
      "rewardPoint": 200,
      "couponId": null,
      "rewardQuantity": 1
    }
  ]
}
```

`challengeType`은 `TOTAL`, `status`는 `INACTIVE` — 둘 다 허용값 안의 정상 케이스로 바꿔서, `@Pattern` 추가가 **정상 케이스까지 막아버리는 회귀**가 없는지 확인한다.

기대:

- `200 OK`
- 응답 `data.challengeId`가 `testChallengeId`와 동일
- 실제로 다르게 나오면 어디를 봐야 하는지:
  - **`400 VALIDATION_ERROR`가 나오면** → `@Pattern`의 `regexp`가 잘못됐을 가능성이 크다(예: 대소문자, 오타, `|` 구분자 누락). `AdminChallengeUpdateRequest.java`의 `regexp = "DAILY|TOTAL"` / `regexp = "ACTIVE|INACTIVE"` 문자열을 다시 확인.

사후 DB 확인(수정 내용이 반영됐어야 정상):

```sql
SELECT c.challenge_id, c.challenge_name, c.challenge_type, c.target_action, c.target_count, c.status,
       cr.challenge_reward_id, cr.reward_type, cr.reward_point, cr.reward_quantity
FROM challenge c
LEFT JOIN challenge_reward cr ON cr.challenge_id = c.challenge_id
WHERE c.challenge_id = @test_challenge_id;
```

확인 기준: `challenge_type='TOTAL'`, `status='INACTIVE'`, `challenge_name`이 `_수정`으로 끝나는 이름으로 바뀌어 있고, `challenge_reward.reward_point=200`으로 갱신돼 있어야 한다.

> **#1~#3을 전부 마쳤다면**: SQL 파일 **§3(정리)**로 이동해서 실행한다(이 블록 안에서 `SQL_SAFE_UPDATES`를 SQL 파일이 알아서 껐다 켠다). 이번에 만든 테스트 전용 행만 지우는 것이고 기존 데이터는 애초에 안 건드렸으므로 원복할 게 따로 없다. `remainingRewardCount = 0`, `remainingChallengeCount = 0`인지 확인한다.

### 실행 결과 요약 (2026-07-08 완료)

| 케이스 | 사용된 challenge_id | 응답 | 판정 |
|---|---|---|---|
| #1 challengeType 위반(`"WEEKLY"`) | 79 | 400 `VALIDATION_ERROR`, `field="challengeType"`, "챌린지 유형은 DAILY 또는 TOTAL만 허용됩니다." / 사후 DB: `DAILY`/`ACTIVE` 그대로(미반영) | **PASS** |
| #2 status 위반(`"DELETED"`) | 80 | 400 `VALIDATION_ERROR`, `field="status"`, "상태는 ACTIVE 또는 INACTIVE만 허용됩니다." / 사전·사후 DB 동일(미반영) | **PASS** |
| #3 정상값 회귀(`TOTAL`/`INACTIVE`) | 80 | 200 `OK`, `challengeId=80` / DB에 `challenge_type='TOTAL'`, `status='INACTIVE'`, `reward_point=200` 전부 반영 | **PASS** |

**공정 상 특이사항**: SQL §1을 두 번 실행해서(1차 백업 방식 시도 때 1번, 단순화한 스크립트로 다시 시작할 때 1번) 테스트 전용 챌린지가 79, 80 두 개 생겼다. #1은 79로, #2·#3은 80으로 테스트됐다 — 둘 다 `SWAGGER_TEST_0708_` 접두사가 붙은 테스트 전용 행이라 실제 데이터에는 영향 없지만, 정리할 때 80뿐 아니라 79도 같이 지워야 한다(§5 "잔여 테스트 데이터 정리" SQL 참고).

---

## 4. 저장할 증거

```text
api-test-results/챌린지출석포인트쿠폰 API 0708/#1 challengeType 위반/400.json
api-test-results/챌린지출석포인트쿠폰 API 0708/#2 status 위반/400.json
api-test-results/챌린지출석포인트쿠폰 API 0708/#3 정상값 회귀/200.json
api-test-results/챌린지출석포인트쿠폰 API 0708/#3 정상값 회귀/사후 DB.png
```

## 5. 최종 체크리스트

| 항목 | 완료 |
|---|---|
| (이전에 백업/전체비움 방식을 시도하다 멈췄다면) SQL 파일 상단 복구 블록 실행 — `challenge_reward` 원복 확인 후 백업 테이블 삭제 | [x] |
| SQL §0 실행 — 관리자 승격 + `adminId` 확인 | [x] |
| SQL §1 실행 — 테스트 전용 챌린지/보상 추가, `testChallengeId` 확보 | [x] |
| #1 `challengeType="WEEKLY"` → 400 `VALIDATION_ERROR` | [x] (challenge_id=79, `api-test-results/.../challengeType 허용값 위반/400.json` 확인) |
| #2 `status="DELETED"` → 400 `VALIDATION_ERROR` | [x] (challenge_id=80, `.../status 허용값 위반/400.json` 확인) |
| #3 정상값(`TOTAL`/`INACTIVE`) → 200, DB 반영 확인 | [x] (challenge_id=80, `.../정상값 회귀 확인/200.json` + 사후 db.txt 확인) |
| SQL §3 실행 — 정리 확인(`remainingRewardCount`/`remainingChallengeCount` 둘 다 0) | [ ] **주의: §1을 두 번 실행해서 challenge_id 79/80 두 개가 생겼다. §3은 `@test_challenge_id`(현재 80)만 지우므로, 79도 별도로 지워야 한다 — 아래 정리 SQL 참고** |

**잔여 테스트 데이터 정리(79 + 80 둘 다)**:

```sql
SET SQL_SAFE_UPDATES = 0;
DELETE FROM challenge_reward WHERE challenge_id IN (79, 80);
DELETE FROM challenge WHERE challenge_id IN (79, 80);
SET SQL_SAFE_UPDATES = 1;

SELECT COUNT(*) AS remainingCount
FROM challenge
WHERE challenge_id IN (79, 80);
-- 0이어야 정상. (더 안전하게 하려면 challenge_name LIKE 'SWAGGER_TEST_0708_%'로 조회해서
-- 남은 게 없는지 한 번 더 확인한다.)
```

## 6. PR #33 반영 문구 (테스트 완료 후 사용)

```markdown
- [x] `AdminChallengeUpdateRequest.challengeType`/`status` 허용값 검증 Swagger 확인
  - `challengeType`/`status`에 CHECK 제약 밖 값을 넣으면 400 `VALIDATION_ERROR`로 응답(이전엔 500 위험)
  - 정상값(`DAILY`/`TOTAL`, `ACTIVE`/`INACTIVE`)은 기존과 동일하게 200으로 반영되는 회귀 없음 확인
```
