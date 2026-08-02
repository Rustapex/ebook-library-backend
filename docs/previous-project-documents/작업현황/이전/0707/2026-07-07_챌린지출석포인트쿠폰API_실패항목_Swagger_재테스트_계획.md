# 챌린지/출석/포인트/쿠폰 API 실패 항목 Swagger 재테스트 계획 (개정판)

작성일: 2026-07-07 (개정)
관련 SQL: `api-test-results/챌린지출석포인트쿠폰 API/swagger/TEST 진행시 SQL/2026-07-07_재테스트_실행.sql`

## 0. 이 개정판에서 고친 것 (계속 실패하던 원인)

기존 문서로 재테스트가 계속 실패한 원인을 실제 증거 파일과 코드 기준으로 확인했다.

1. **§3.4(구) 세션 변수의 로그인 ID가 실제 계정과 다름.** 기존 문서는 `SET @login_id_a = 'sys1009';`를 썼는데, 실제 회원 A 계정은 `testusera`(memberId 69, `JWT 회원가입/201-user1.json` 근거)다. `sys1009`는 `2026-07-07_test-env-setup.sql`에 남아있는 예전 라운드 계정 이름으로 추정된다. 이 값이 틀리면 `@member_id_a`가 `NULL`이 되고, 이후 모든 절의 쿼리가 연쇄적으로 빈 결과를 반환한다 — "DB 쿼리가 이상하다"의 실제 원인으로 보인다. 이번 개정판과 SQL 파일은 `testusera`/`testuserb`/`adminuser`를 로그인 ID로 직접 조회해서 쓴다(§2.3).
2. **API-ATTEND-002 재시도(17:33 캡처)가 코드 수정 이후인데도 여전히 500.** `.\gradlew.bat build`는 컴파일 + 테스트만 하고, 이미 떠 있는 `bootRun` 서버 프로세스를 재시작하지 않는다. 서버를 재시작하지 않고 재테스트하면 이전 빌드가 계속 응답한다 — §2.1에 "서버 재시작 필수"를 명시했다.
3. **API-CHALLENGE-002 2차시(17:40) 409는 새 버그가 아니라 이전 진행도 재사용 때문.** 1차 시도(12:27, 코드 수정 전)에서 이미 `reward_claimed_at`이 채워진 같은 `challenge_progress` 행을 2차 시도에서 재사용해서 당연히 "이미 수령한 보상"(409)이 나온 것이다. 새 챌린지로 처음부터 다시 검증해야 한다(기존 문서 §5.2 방식이 맞는 접근이라 이번에도 유지, SQL 파일로 이관).
4. **사전/사후 DB 확인 쿼리를 절마다 3~4개씩 따로 실행하던 것을 절마다 1~2개로 합쳤다.** 사전 확인은 "데이터 준비 + 스냅샷 캡처 + 확인용 SELECT"를 한 번에 실행하고, 사후 확인은 사전 값과 사후 값을 같은 행에서 비교하는 SELECT 하나로 합쳤다(§B의 SQL 파일, 아래 §4 참고).
5. **이미 성공(오탐으로 결론난) 2개 항목을 재테스트 대상에서 제외했다.** API-ADMIN-CHALLENGE-002, API-ADMIN-COUPON-002는 이전 세션에서 실제 DB 재조회로 정상 반영을 이미 확인했다(§2 참고). 재테스트할 필요 없이 완료 처리한다.

---

## 1. 재테스트 대상 (갱신)

| 순서 | API ID (Method + URL) | 상태 | 비고 |
|---|---|---|---|
| 1 | API-ATTEND-002 (`GET /api/v1/me/attendance?yearMonth=`) | **재테스트 필요** | 500이 두 번 재현됨(11:50 수정 전, 17:33 "재시도"). 재시도가 서버 재시작 없이 이뤄졌을 가능성이 높아 재검증 필요 |
| 2 | API-CHALLENGE-002 (`POST /api/v1/challenges/{claimChallengeId}/reward`) | **재테스트 필요** | 1차(12:27, 수정 전) `grantedPoint=0`, 2차(17:40)는 이미 수령 처리된 진행도를 재사용해 409 — 수정된 코드로 새 챌린지를 처음부터 검증한 적이 아직 없음 |
| 3 | API-ADMIN-CHALLENGE-002 (`PUT /api/v1/admin/challenges/{challengeId}`) | **성공(완료, 재테스트 불필요)** | 응답 200, `challengeId=10`. 이전 세션에서 실제 DB 재조회로 `challenge_reward.reward_point=3500` 반영을 확인함 — "사후 DB 미반영" 캡처는 조회 타이밍 오탐으로 결론 |
| 4 | API-ADMIN-COUPON-002 (`POST /api/v1/admin/coupons`) | **성공(완료, 재테스트 불필요)** | 응답 201, `couponId=10`. 이전 세션에서 실제 DB 재조회로 요청값과 일치함을 확인함 — 마찬가지로 오탐으로 결론 |

이번 문서에서 실제로 다시 실행해야 하는 항목은 **1번, 2번 두 건뿐**이다.

---

## 2. 공통 준비 (갱신)

### 2.1 코드 수정 반영 — 서버 재시작 필수

```powershell
# 기존에 bootRun으로 떠 있는 프로세스가 있으면 먼저 Ctrl+C로 종료한 뒤 다시 실행한다.
.\gradlew.bat bootRun --console=plain
```

`.\gradlew.bat build`만 실행하고 서버를 재시작하지 않으면 코드 수정이 반영되지 않은 채로 재테스트하게 된다. 재테스트 시작 전에 반드시 서버가 최신 코드로 새로 떠 있는지 확인한다.

### 2.2 Swagger 접속

```text
http://localhost:8080/swagger-ui/index.html
```

### 2.3 인증 계정 (실제 값 — `JWT 회원가입/테스트 데이터 준비.txt` 기준)

| 계정 | login_id | memberId | 역할 |
|---|---|---|---|
| 회원 A | `testusera` | 69 | USER |
| 회원 B | `testuserb` | 70 | USER |
| 관리자 | `adminuser` | 71 | ADMIN (회원가입 직후엔 USER — DB에서 승격 필요, SQL 파일 0절 참고) |

기존 문서의 `sys1009`/`wes6027`은 실제 계정이 아니다. **더 이상 사용하지 않는다.**

Swagger 우측 상단 `Authorize`에 `Bearer {JWT}` 형식으로 입력한다.

### 2.4 DB 세션 변수 및 테스트용 SQL

`api-test-results/챌린지출석포인트쿠폰 API/swagger/TEST 진행시 SQL/2026-07-07_재테스트_실행.sql` 파일을 열어 0 → 1 → 2(또는 4)절 순서대로 **같은 Workbench 탭에서 끊지 않고** 실행한다. 이 문서에는 SQL을 중복 기재하지 않는다.

---

## 3. API-ATTEND-002 (`GET /api/v1/me/attendance?yearMonth=`) 재테스트

### 3.1 이전 실패

- 500이 두 번(11:50 수정 전, 17:33 재시도) — §2.1의 서버 재시작을 거치지 않고 재시도했을 가능성이 있다.

### 3.2 Swagger 요청

```text
GET /api/v1/me/attendance?yearMonth=2026/07
Authorization: Bearer {회원 A(testusera) JWT}
```

`2026/07`은 `yyyy-MM` 형식이 아니므로 형식 오류 케이스로 사용한다.

### 3.3 기대 응답

```json
{
  "code": "VALIDATION_ERROR",
  "success": false
}
```

상태 코드는 400이어야 한다. 500이면 실패이며, 이 경우 가장 먼저 §2.1(서버 재시작)부터 다시 확인한다.

### 3.4 DB 확인

필요 없음 — 읽기 전용(GET) 요청이라 DB를 변경하지 않는다(SQL 파일 §4 참고).

### 3.5 저장할 증거

```text
api-test-results/챌린지출석포인트쿠폰 API/swagger/API-ATTEND-002 GET/yearMonth 형식 오류/재검증/400.json
```

---

## 4. API-CHALLENGE-002 (`POST /api/v1/challenges/{claimChallengeId}/reward`) 재테스트

### 4.1 이전 실패

- 1차(12:27, 코드 수정 전): 200 응답이지만 `grantedPoint=0` — `challenge_reward`가 대상 챌린지에 연결되지 않아 포인트 지급 루프가 실행되지 않음
- 2차(17:40): 1차 시도에서 이미 `reward_claimed_at`이 채워진 같은 진행도를 재사용해 409 `CHALLENGE_REWARD_NOT_CLAIMABLE` — 새 버그가 아니라 데이터 재사용 문제

### 4.2 사전 준비 + 사전 스냅샷 (SQL 파일 §2-1)

`TEST 진행시 SQL/2026-07-07_재테스트_실행.sql`의 "2-1. 사전 준비 + 사전 스냅샷"을 실행한다. 이번 재검증 전용 새 챌린지를 만들고 회원 A를 "달성 + 미수령" 상태로 세팅하면서, 동시에 사전 포인트 잔액을 캡처해 한 화면에 보여준다. 출력된 `claimChallengeId` 값을 다음 Swagger 요청의 path variable로 사용한다.

### 4.3 Swagger 요청

```text
POST /api/v1/challenges/{claimChallengeId}/reward
Authorization: Bearer {회원 A(testusera) JWT}
```

Path variable: `challengeId` = 위에서 조회한 `claimChallengeId` 값. Request body 없음.

### 4.4 기대 응답

```json
{
  "code": "OK",
  "data": {
    "rewardClaimedAt": "값 존재",
    "grantedPoint": 100
  },
  "success": true
}
```

`grantedPoint`가 0이면 실패다.

### 4.5 사후 확인 (SQL 파일 §2-2)

같은 SQL 파일의 "2-2. 사후 확인"을 실행한다. 사전 잔액과 사후 잔액, `reward_claimed_at`, 최신 `point_history` 행을 한 SELECT로 같이 보여준다.

합격 기준(전부 한 줄에서 확인 가능):

- `pointBalanceDelta = 100`
- `reward_claimed_at IS NOT NULL`
- `point_type = 'challenge_reward'`
- `point_amount = 100`

### 4.6 저장할 증거

```text
api-test-results/챌린지출석포인트쿠폰 API/swagger/API-CHALLENGE-002 POST/달성 + 미수령 보상 수령 재검증/사전.png
api-test-results/챌린지출석포인트쿠폰 API/swagger/API-CHALLENGE-002 POST/달성 + 미수령 보상 수령 재검증/200.json
api-test-results/챌린지출석포인트쿠폰 API/swagger/API-CHALLENGE-002 POST/달성 + 미수령 보상 수령 재검증/사후.png
```

### 4.7 정리(원복) — 필수

SQL 파일의 "2-3. 원복"을 실행해 이번 테스트로 만든 챌린지/보상/진행도/포인트이력만 지우고, 회원 A의 포인트 잔액을 테스트 전 값으로 되돌린다. Workbench 세션이 끊겨 변수를 잃어버렸다면 SQL 파일 "3. 세션이 끊겼을 때" 블록(이름 패턴 기반 예비 경로)을 사용한다.

---

## 5. 이번 문서에서 제외하는 항목 (동시성 등, 기존 결정 유지)

아래 항목은 이전 Swagger 단건 실패 증거가 아니라 코드 리뷰로 확인한 위험 또는 단위 테스트로 검증할 항목이므로, 이번 Swagger 재테스트 문서의 필수 범위에서 제외한다.

| 항목 | 제외 이유 |
|---|---|
| 챌린지 보상 수령 동시 요청 중복 지급 | Swagger 단건 실행만으로 동시성 재현이 어렵다. Postman Runner/동시 요청 도구가 필요하다. |
| 출석 체크 동시 요청 409 보장 | Swagger 단건 실행만으로 동시성 재현이 어렵다. Postman Runner/동시 요청 도구가 필요하다. |
| 관리자 포인트 조정 `pointAmount` 누락 | 기존 Swagger 실패 파일이 아니라 코드 리뷰로 확인한 검증 누락이다. 필요하면 별도 회귀 테스트로 추가한다. |
| `rewardType=coupon` 등록 차단 | 코드 리뷰 대응으로 정책이 바뀐 항목이다(`docs/작업현황/2026-07-07_PR33_코드리뷰_대응계획.md` HIGH-1 참고). 이전 실패 항목 재검증과 별도로 확인한다. |

---

## 6. 최종 체크리스트 (갱신)

| 항목 | 완료 |
|---|---|
| API-ATTEND-002 yearMonth 형식 오류가 400 `VALIDATION_ERROR`로 응답 | [ ] |
| API-CHALLENGE-002 보상 수령 시 `grantedPoint > 0` | [ ] |
| API-CHALLENGE-002 보상 수령 후 `reward_claimed_at` 반영 | [ ] |
| API-CHALLENGE-002 보상 수령 후 `point_history.point_type='challenge_reward'` 반영 | [ ] |
| API-ADMIN-CHALLENGE-002 정상 수정 후 `challenge_reward.reward_point=3500` 반영 | [x] (완료, 재테스트 불필요 — §1 참고) |
| API-ADMIN-COUPON-002 정상 등록 후 응답 `couponId` 기준 DB 행 확인 | [x] (완료, 재테스트 불필요 — §1 참고) |

---

## 7. PR 본문 반영 문구

Swagger 재테스트 완료 후 PR 본문의 테스트/검증 항목에는 아래처럼 적는다.

```markdown
- [x] Swagger 재검증 확인
  - API-ATTEND-002: 잘못된 `yearMonth` 요청이 500이 아니라 400 `VALIDATION_ERROR`로 응답되는지 확인
  - API-CHALLENGE-002: 달성 + 미수령 보상 수령 시 `grantedPoint`, `reward_claimed_at`, `point_history.point_type='challenge_reward'` 반영 확인
  - API-ADMIN-CHALLENGE-002: 이전 세션에서 DB 재조회로 정상 반영 확인 완료(오탐 판명), 재테스트 생략
  - API-ADMIN-COUPON-002: 이전 세션에서 DB 재조회로 정상 반영 확인 완료(오탐 판명), 재테스트 생략
```
