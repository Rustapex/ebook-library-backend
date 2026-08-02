# 챌린지/출석/포인트/쿠폰 API Postman 동시성/검증 계획

작성일: 2026-07-07
실행 주체: **사용자가 Postman 앱에서 직접 실행**(이 문서와 SQL 파일은 무엇을 어떻게 실행할지만 안내한다)
관련 SQL: `api-test-results/챌린지출석포인트쿠폰 API/postman/2026-07-07_Postman_동시성검증_실행.sql`
DDL/시드 기준: `src/main/resources/schema.sql`(스키마), `src/main/resources/data.sql`(member_grade만 시드 — 챌린지/쿠폰/출석/포인트 관련 시드는 없음)
계정 정보: `api-test-results/챌린지출석포인트쿠폰 API/swagger/JWT 회원가입/테스트 데이터 준비.txt`(테스트 전용 계정, 잔여 데이터가 남아도 무방)

## 0. 배경

`docs/작업현황/2026-07-07_챌린지출석포인트쿠폰API_실패항목_Swagger_재테스트_계획.md` §5("이번 문서에서 제외하는 항목")에서 아래 4개 항목을 "Swagger 단건 실행으로는 검증 불가"로 분류하고 별도 문서로 미뤄뒀다. 이 문서가 그 후속이다.

| 항목 | Swagger 재테스트 문서에서 제외한 이유 |
|---|---|
| 챌린지 보상 수령 동시 요청 중복 지급 | Swagger 단건 실행만으로 동시성 재현이 어렵다. |
| 출석 체크 동시 요청 409 보장 | Swagger 단건 실행만으로 동시성 재현이 어렵다. |
| 관리자 포인트 조정 `pointAmount` 누락 | 코드 리뷰로 확인한 검증 누락이라 별도 회귀 테스트가 필요하다. |
| `rewardType=coupon` 등록 차단 | 코드 리뷰 대응으로 정책이 바뀐 항목이라 별도로 확인해야 한다. |

전제: DB는 `schema.sql`+`data.sql`만 실행되고 회원 3명(`testusera`/`testuserb`/`adminuser`)만 만들어진 상태로 가정한다. 이전 라운드의 challenge_id/coupon_id 값에 의존하지 않는다. 이미 데이터가 있어도 무방하다 — 이번 테스트가 직접 건드리는 부분(특히 회원 B의 오늘자 출석 기록)만 SQL 파일이 백업 후 지웠다가 테스트가 끝나면 복원한다.

---

## 1. Postman Environment

Environment 이름 예시: `reward-api-local`

**JWT 값을 Environment 편집창에 직접 타이핑/붙여넣기 하지 않는다.** Postman은 Value 칸에 JWT처럼 생긴 문자열을 손으로 입력하면 "민감한 데이터가 감지됐습니다. secret으로 저장하세요" 경고를 띄운다. 대신:

1. `jwtUserA`/`jwtUserB`/`jwtAdmin` 세 변수는 Type을 `default`가 아니라 **`secret`**으로 지정한다(Environment 편집 화면에서 변수 행 왼쪽 Type 드롭다운 변경 — 값이 자물쇠 아이콘으로 마스킹됨).
2. Initial Value/Current Value는 **비워둔 채로 저장**한다.
3. 값은 아래 §2 "0. 인증" 폴더의 로그인 요청 3개가 응답을 받을 때 **스크립트로만** 채운다(사람이 직접 입력한 적이 없으므로 경고가 뜨지 않는다).

이렇게 하면 계정을 바꿔가며 로그인할 때도 Environment 값 칸을 열 필요 없이, 해당 계정의 로그인 요청만 다시 Send하면 된다.

| Variable | Type | Initial/Current Value | 비고 |
|---|---|---|---|
| `baseUrl` | default | `http://localhost:8080` | |
| `jwtUserA` | **secret** | (비워둠 — 로그인 요청이 자동으로 채움) | 회원 A(`testusera`) 전용 |
| `jwtUserB` | **secret** | (비워둠 — 로그인 요청이 자동으로 채움) | 회원 B(`testuserb`) 전용 |
| `jwtAdmin` | **secret** | (비워둠 — 로그인 요청이 자동으로 채움) | `adminuser` 전용(§0 SQL로 ADMIN 승격 먼저 실행 필요) |
| `claimChallengeId` | default | (SQL 파일 §2-1 실행 후 나온 값) | 시나리오 1(챌린지 보상 동시 수령)에서 사용 |
| `attendanceMemberId` | default | 회원 B의 `memberId` (SQL 파일 §1 실행 결과의 `memberIdB`) | 시나리오 2 참고용(요청 자체는 JWT로 인증되므로 body/path에는 안 쓰이지만 기록용) |
| `adminTargetMemberId` | default | (SQL 파일 §4-1 실행 후 나온 값) | 시나리오 3(포인트 조정 검증)에서 사용 |
| `validationChallengeId` | default | (SQL 파일 §5-1 실행 후 나온 값) | 시나리오 4(coupon 등록 차단 검증)에서 사용 |

---

## 2. Collection 파일 구조

Collection 이름 예시: `챌린지-출석-포인트-쿠폰 동시성-검증`

```
챌린지-출석-포인트-쿠폰 동시성-검증/
├── 0. 인증 (계정별로 각각 별도 요청 — 아래 표 참고)
│   ├── 회원A 로그인          POST {{baseUrl}}/api/v1/auth/login
│   ├── 회원B 로그인          POST {{baseUrl}}/api/v1/auth/login
│   └── 관리자 로그인         POST {{baseUrl}}/api/v1/auth/login
│
├── 1. 챌린지 보상 동시 수령
│   └── 보상 수령 요청         POST {{baseUrl}}/api/v1/challenges/{{claimChallengeId}}/reward
│                             Authorization: Bearer {{jwtUserA}}
│                             (요청은 1개만 만든다 — Collection Runner로 2회 반복 실행, §3 참고)
│
├── 2. 출석 체크 동시 요청
│   └── 출석 체크 요청         POST {{baseUrl}}/api/v1/me/attendance
│                             Authorization: Bearer {{jwtUserB}}
│                             (요청은 1개만 만든다 — Collection Runner로 2회 반복 실행, §3 참고)
│
├── 3. 관리자 포인트 조정 pointAmount 누락
│   └── pointAmount 누락      POST {{baseUrl}}/api/v1/admin/members/{{adminTargetMemberId}}/points
│                             Authorization: Bearer {{jwtAdmin}}
│                             Body: { "description": "포인트 조정 검증 테스트" }   ← pointAmount 필드 자체를 뺌
│
└── 4. 관리자 챌린지 수정 rewardType=coupon 차단
    └── coupon 등록 시도       PUT {{baseUrl}}/api/v1/admin/challenges/{{validationChallengeId}}
                              Authorization: Bearer {{jwtAdmin}}
                              Body 예시:
                              {
                                "challengeName": "쿠폰차단 검증용",
                                "challengeType": "TOTAL",
                                "targetAction": "ATTENDANCE",
                                "targetCount": 10,
                                "status": "ACTIVE",
                                "rewards": [
                                  { "rewardType": "coupon", "rewardPoint": null, "couponId": null, "rewardQuantity": 1 }
                                ]
                              }
```

### 0. 인증 폴더 상세 (계정 3개, 각각 별도 요청으로 만든다)

| 요청 이름 | Method/URL | Body | Tests(Post-response) 스크립트 |
|---|---|---|---|
| 회원A 로그인 | `POST {{baseUrl}}/api/v1/auth/login` | `{ "loginId": "testusera", "password": "testusera0!" }` | `pm.environment.set("jwtUserA", pm.response.json().data.accessToken);` |
| 회원B 로그인 | `POST {{baseUrl}}/api/v1/auth/login` | `{ "loginId": "testuserb", "password": "testuserb0!" }` | `pm.environment.set("jwtUserB", pm.response.json().data.accessToken);` |
| 관리자 로그인 | `POST {{baseUrl}}/api/v1/auth/login` | `{ "loginId": "adminuser", "password": "adminuser0!" }` | `pm.environment.set("jwtAdmin", pm.response.json().data.accessToken);` |

계정마다 요청을 따로 만들어두면, 특정 계정의 토큰만 다시 받고 싶을 때(예: 만료) Environment 값 칸을 열지 않고 그 로그인 요청 하나만 다시 Send하면 된다. 비밀번호는 `JWT 회원가입/테스트 데이터 준비.txt` 기준(테스트 전용 계정).

`0. 인증` 폴더의 3개 요청은 매번 맨 처음에 한 번씩 실행해서 JWT를 채운다(§1 Environment 표 참고 — 값은 위 스크립트로만 채워지고 직접 입력하지 않는다).

---

## 3. 동시 요청을 실제로 어떻게 보내는지 (Collection Runner 방식)

사람이 마우스로 `Send`를 두 번 누르는 방식은 재현성이 낮아 채택하지 않는다. 대신 **Postman Collection Runner로 같은 요청 1개를 Iterations=2, Delay=0ms로 반복 실행**한다.

### 테스트 방식

1. 테스트할 요청을 Collection에 저장한다(§2의 "보상 수령 요청" 또는 "출석 체크 요청" — 폴더당 요청 1개만 있으면 된다).
2. 해당 요청에 Authorization이 적용돼 있는지 확인한다.
   - Bearer Token 탭에 `{{jwtUserA}}`(또는 `{{jwtUserB}}`) 지정, 또는
   - Header에 `Authorization: Bearer {{jwtUserA}}` 직접 추가.
3. Collection Runner를 연다(Collection 우클릭 → Run collection, 또는 Runner 아이콘).
4. 실행할 요청은 **테스트 대상 API 1개만** 체크한다(다른 요청은 전부 체크 해제).
5. **Iterations를 2로 설정**한다.
6. **Delay를 0ms로 설정**한다.
7. Run을 실행한다.
8. 결과 화면에서 두 번의 실행 각각의 status code와 response body를 확인한다.
9. 동시에 스프링 부트 콘솔 로그에서 실제 예외 stack trace가 찍히는지 같이 확인한다(500이 나온 경우 특히 중요).

### 주의

- 이 방식은 사람이 Send 버튼을 두 번 누르는 것보다 재현성이 높다.
- 다만 Postman Runner의 0ms delay가 완벽한 병렬 요청을 보장하는 것은 아니다 — 요청 사이 대기시간 없이 빠르게 연속 실행하는 방식에 가깝다.
- 그래도 출석 체크, 보상 수령, 포인트 지급, 쿠폰 발급처럼 중복 요청에 취약한 API에서는 동시성 문제를 발견하는 데 도움이 된다.
- 만약 이 방식에서 500이 발생하면 Postman/인증 문제라기보다는 서버 코드의 동시성 처리, DB unique 제약, 트랜잭션 처리, 예외 처리 문제일 가능성이 크다 — 콘솔 stack trace부터 확인한다.
- 재현이 잘 안 되면 Iterations를 5~10으로 늘려 여러 번 반복하고, 매번 "정확히 1개만 성공"하는지 확인한다.

---

## 4. 시나리오별 실행 순서

### 4-1. 챌린지 보상 동시 수령

1. SQL 파일 §0, §1 실행(계정 준비 + 세션 변수).
2. SQL 파일 §2-1 실행 → 출력된 `claimChallengeId`를 Postman `claimChallengeId` 변수에 넣는다.
3. Postman `0. 인증 > 회원A 로그인` 실행(`jwtUserA` 채움).
4. Collection Runner에서 `1. 챌린지 보상 동시 수령 > 보상 수령 요청` 1개만 선택, Iterations=2, Delay=0ms로 Run(§3 참고).
5. 결과 확인: 2회 중 정확히 1개만 `200 OK` + `grantedPoint=100`, 나머지 1개는 `409 CHALLENGE_REWARD_NOT_CLAIMABLE`. 500이 있으면 콘솔 stack trace부터 확인.
6. SQL 파일 §2-2 실행(사후 확인) → `pointBalanceDelta=100`, `pointHistoryRowCount=1` 확인.
7. **SQL 파일 §2-3(원복) 반드시 실행.**

### 4-2. 출석 체크 동시 요청

1. SQL 파일 §3-1 실행(회원 B의 오늘자 출석 기록 백업 + 삭제, `backedUpRowCount` 확인).
2. Postman `0. 인증 > 회원B 로그인` 실행(`jwtUserB` 채움).
3. Collection Runner에서 `2. 출석 체크 동시 요청 > 출석 체크 요청` 1개만 선택, Iterations=2, Delay=0ms로 Run(§3 참고).
4. 결과 확인: 2회 중 정확히 1개만 `201 Created`, 나머지 1개는 `409 ATTENDANCE_ALREADY_EXISTS`. 500이 있으면 콘솔 stack trace부터 확인.
5. SQL 파일 §3-2 실행(사후 확인) → `attendanceRowCountToday=1`.
6. **SQL 파일 §3-3(원복) 반드시 실행** — 백업해둔 원래 출석 기록이 있었다면 복원, 없었다면 그대로 빈 상태로 남긴다.

### 4-3. 관리자 포인트 조정 `pointAmount` 누락

1. SQL 파일 §4-1 실행 → 출력된 `adminTargetMemberId`를 Postman 변수에 넣고, `pointBalanceBefore` 값을 기록해둔다.
2. Postman `0. 인증 > 관리자 로그인` 실행(`jwtAdmin` 채움, §0 SQL로 ADMIN 승격이 먼저 돼 있어야 함).
3. Postman `3. 관리자 포인트 조정 pointAmount 누락` 요청 Send(body에 `pointAmount` 필드 자체가 없어야 함).
4. 응답 확인: `400 VALIDATION_ERROR`.
5. SQL 파일 §4-2 실행(사후 확인) → 잔액이 `pointBalanceBefore`와 정확히 같은지 확인(다르면 검증 실패 상태에서도 DB가 바뀐 것이므로 심각한 버그).
6. 원복 불필요(DB가 변하지 않아야 정상이므로 정리할 게 없음).

### 4-4. 관리자 챌린지 수정 `rewardType=coupon` 등록 차단

1. SQL 파일 §5-1 실행 → 출력된 `validationChallengeId`를 Postman 변수에 넣는다.
2. Postman `4. 관리자 챌린지 수정 rewardType=coupon 차단` 요청 Send.
3. 응답 확인: `400 VALIDATION_ERROR`.
4. SQL 파일 §5-2 실행(사후 확인) → `couponRewardRowCount=0`.
5. **SQL 파일 §5-3(원복) 반드시 실행** — 검증용으로 만든 챌린지를 지운다.

---

## 5. 최종 체크리스트

| 항목 | 완료 |
|---|---|
| 챌린지 보상 동시 수령: 정확히 1건만 성공(200), 나머지는 409 | [ ] |
| 챌린지 보상 동시 수령: `point_history` 중복 지급 없음(`pointHistoryRowCount=1`) | [ ] |
| 출석 체크 동시 요청: 정확히 1건만 성공(201), 나머지는 409 | [ ] |
| 출석 체크 동시 요청: `attendance` 중복 행 없음(`attendanceRowCountToday=1`) | [ ] |
| 관리자 포인트 조정 `pointAmount` 누락 시 400 `VALIDATION_ERROR`, 잔액 불변 | [ ] |
| 관리자 챌린지 수정 `rewardType=coupon` 등록 시 400 `VALIDATION_ERROR`, DB 미반영 | [ ] |
| 테스트 종료 후 SQL 파일의 모든 원복 절차 실행 완료 | [ ] |

## 6. PR #33 반영 문구 (테스트 완료 후 사용)

```markdown
- [x] Postman 동시성/검증 확인
  - 챌린지 보상 수령 동시 요청 시 1건만 성공, 나머지는 409 `CHALLENGE_REWARD_NOT_CLAIMABLE` 확인
  - 출석 체크 동시 요청 시 1건만 성공, 나머지는 409 `ATTENDANCE_ALREADY_EXISTS` 확인
  - 관리자 포인트 조정 `pointAmount` 누락 시 400 `VALIDATION_ERROR`, DB 미변경 확인
  - 관리자 챌린지 수정 `rewardType=coupon` 등록 시 400 `VALIDATION_ERROR`, DB 미반영 확인
```
