# `claimReward()` coupon 보상 fail-fast — Swagger 검증 체크리스트

작성일: 2026-07-09
관련 PR: https://github.com/shangpa/AcornProject/pull/35 (`fix/challenge-coupon-reward-guard`)
관련 계획: `docs/작업현황/2026-07-09_PR33_5차리뷰_및_후속작업_대응계획.md` §3, `docs/작업현황/2026-07-09_fix-challenge-coupon-reward-guard_구현계획.md`
대상 코드: `ChallengeService.claimReward()` — 이미 코드 반영·단위테스트 통과 완료(PR #35), 이 문서는 그 위에 Swagger로 실제 API 호출까지 확인하는 절차.

---

## 0. 이번에 뭘 테스트하고, 뭘 안 하는지 (범위 명시 — 중요)

**원칙: 이 PR에서 실제로 코드가 바뀐 부분만 테스트한다.** PR #35의 diff를 기준으로 판단하면:

| 변경 파일 | 프로덕션 코드 변경? | Swagger/Postman 필요? |
|---|---|---|
| `ChallengeService.claimReward()` | **예** — `POINT`가 아닌 타입을 만나면 조용히 넘어가던 걸 `else`로 예외를 던지게 바꿈 | **필요 — 이번 체크리스트의 대상** |
| `ErrorCode.java` | 예(enum 상수 추가) — 위 변경에 종속돼서만 동작하므로 별도 호출 경로 없음 | 위 항목과 같이 확인됨(응답 body의 `code` 필드로 같이 확인) |
| `ChallengeService.claimReward()`의 기존 `POINT` 지급 분기(`if` 블록) | **아니오** — 이번 PR에서 한 글자도 안 바뀜 | 불필요. 기존 회귀 테스트(`ChallengeServiceTest.claimReward_보상은_달성시점이_아닌_수령시점_최신값을_지급한다`)로 이미 커버됨, 별도 재확인 안 함 |
| `markRewardClaimedIfUnclaimed()`(리포지토리 메서드 본체) | **아니오** — 이번 PR은 이 메서드를 호출하는 순서/방식을 안 바꿨고, 새 단위 테스트만 추가함 | 불필요. 실제 동시 요청 시나리오는 기존 Postman 결과(`api-test-results/챌린지출석포인트쿠폰 API/postman/`)로 이미 검증된 상태 그대로, 이번 PR로 달라진 게 없음 |
| `AttendanceController`, `GlobalExceptionHandler` | **아니오** — 둘 다 프로덕션 코드는 무수정, `AttendanceControllerTest`(신규 파일)만 추가됨 | 불필요. 이 컨트롤러의 파라미터 오류 400 응답은 이전 라운드에서 이미 Swagger로 확인된 기존 동작(`api-test-results/챌린지출석포인트쿠폰 API/swagger/API-ATTEND-002 GET/` 참고)이고, 이번 PR은 그 기존 동작을 자동화 테스트로 고정했을 뿐 동작 자체를 바꾸지 않음 |

**결론: Swagger로 확인할 건 딱 1가지 — coupon(또는 POINT 아닌) 보상을 수령 시도했을 때 409가 오고, DB에 이상한 상태(수령됨으로 마킹만 되고 아무것도 안 지급된 상태)가 안 남는지.**

---

## 1. 사전 준비 (기존 데이터 유지 + 신규 행만 추가)

기존 데이터는 건드리지 않고, 이 테스트 전용 챌린지/보상/진행도만 새로 추가한다. 테스트 계정은 기존에 준비된 `la`(회원, `api-test-results/챌린지출석포인트쿠폰 API/swagger/JWT 회원가입/테스트 데이터 준비.txt` 참고) 사용.

```sql
-- 1) 테스트 전용 챌린지 추가 (TOTAL 타입, 이미 달성 가능하게 target_count 낮게)
INSERT INTO challenge (challenge_name, challenge_type, target_action, target_count, status, created_at)
VALUES ('SWAGGER_TEST_0709_쿠폰보상실패테스트', 'TOTAL', 'ATTENDANCE', 1, 'ACTIVE', NOW());
SET @test_challenge_id = LAST_INSERT_ID();

-- 2) 이 챌린지에 coupon 타입 보상 추가 (coupon_id는 NULL이어도 무방 —
--    claimReward()의 fail-fast 분기는 rewardType만 보고 판단하므로 couponId 값은 이 테스트에 영향 없음)
INSERT INTO challenge_reward (challenge_id, reward_type, reward_point, coupon_id, reward_quantity)
VALUES (@test_challenge_id, 'coupon', NULL, NULL, 1);

-- 3) 회원 la의 진행도를 "달성했지만 미수령" 상태로 직접 삽입
SET @member_id_la = (SELECT member_id FROM member WHERE login_id = 'la' LIMIT 1);
INSERT INTO challenge_progress (member_id, challenge_id, progress_date, current_count, achieved_yn, achieved_at, reward_claimed_at)
VALUES (@member_id_la, @test_challenge_id, CURDATE(), 1, 1, NOW(), NULL);

-- 확인
SELECT @test_challenge_id AS testChallengeId, @member_id_la AS memberIdLa;
SELECT challenge_progress_id, member_id, challenge_id, achieved_yn, reward_claimed_at
FROM challenge_progress WHERE challenge_id = @test_challenge_id;
```

> 관리자 등록 API(`PUT /api/v1/admin/challenges/{id}`)로는 `rewardType="coupon"`이 DTO 검증(`@Pattern(regexp = "POINT")`)에서 막히기 때문에, 이 시나리오는 위처럼 **SQL로 직접 데이터를 넣어야만 재현 가능**하다(리뷰 코멘트가 지적한 "수동 데이터가 남아있는 경우"를 그대로 재현하는 것).

---

## 2. Swagger 실행

```text
POST /api/v1/challenges/{testChallengeId}/reward
인증: 회원 la의 JWT
Body: 없음
```

기대 응답:

- **409 `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED`**, 메시지 "아직 지원하지 않는 보상 유형입니다."
- 실제로 다르게 나오면 어디를 봐야 하는지:
  - **`200 OK`가 나오면** → 코드가 fail-fast 이전 버전으로 되돌아간 것(배포/브랜치 확인). `grantedPoint`가 0으로 조용히 성공했을 가능성.
  - **`500`이 나오면** → `ErrorCode` 상수 추가나 `GlobalExceptionHandler` 연결에 문제. `BusinessException` 핸들러(26행)가 정상 동작하는지 확인.
  - **`409 CHALLENGE_REWARD_NOT_CLAIMABLE`(다른 코드)이 나오면** → `markRewardClaimedIfUnclaimed()`가 0을 반환한 것 = 이미 다른 요청이 선점한 상태. 사전 준비의 `reward_claimed_at`이 정말 `NULL`인지 §1 확인 SELECT로 재확인.

---

## 3. 사후 DB 확인 (이 테스트의 핵심 — 트랜잭션 롤백 확인)

```sql
SELECT challenge_progress_id, achieved_yn, reward_claimed_at
FROM challenge_progress
WHERE challenge_id = @test_challenge_id AND member_id = @member_id_la;
```

- **`reward_claimed_at`이 여전히 `NULL`이어야 정상.** 만약 값이 채워져 있다면 트랜잭션 롤백이 실제로는 안 된 것 — `ChallengeService`의 클래스 레벨 `@Transactional` 설정이나 `BusinessException`이 unchecked인지 다시 확인해야 하는 심각한 문제.
- `point_history`/`member.point_balance`에도 변화가 없어야 함(포인트가 조용히 지급되지 않았는지 교차 확인):

```sql
SELECT point_balance FROM member WHERE member_id = @member_id_la;
SELECT * FROM point_history WHERE member_id = @member_id_la ORDER BY point_history_id DESC LIMIT 3;
```

---

## 4. 정리 (테스트 전용 데이터 삭제)

```sql
SET SQL_SAFE_UPDATES = 0;
DELETE FROM challenge_progress WHERE challenge_id = @test_challenge_id;
DELETE FROM challenge_reward WHERE challenge_id = @test_challenge_id;
DELETE FROM challenge WHERE challenge_id = @test_challenge_id;
SET SQL_SAFE_UPDATES = 1;

SELECT COUNT(*) AS remainingCount FROM challenge WHERE challenge_name LIKE 'SWAGGER_TEST_0709_%';
-- 0이어야 정상
```

---

## 5. 저장할 증거

```text
api-test-results/챌린지출석포인트쿠폰 API 0708/(또는 신규 0709 폴더)/coupon 보상 fail-fast/409.json
api-test-results/챌린지출석포인트쿠폰 API 0708/(또는 신규 0709 폴더)/coupon 보상 fail-fast/사후 DB.png
```

## 6. 최종 체크리스트

| 항목 | 완료 |
|---|---|
| §1 사전 준비 SQL 실행 — `testChallengeId`/`memberIdLa` 확보 | [ ] |
| §2 Swagger 호출 — 409 `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED` 확인 | [ ] |
| §3 사후 DB 확인 — `reward_claimed_at` NULL 유지, 포인트 변화 없음 | [ ] |
| §4 정리 SQL 실행 — 잔여 데이터 0건 확인 | [ ] |

## 7. PR #35 반영 문구 (테스트 완료 후 사용)

```markdown
#### 4. 테스트/검증

- [x] 단위 테스트 확인
- [x] 빌드 확인
- [x] Swagger 실제 API 호출 확인
  - coupon 타입 보상을 SQL로 직접 삽입 후 `POST /api/v1/challenges/{id}/reward` 호출 → 409 `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED` 확인
  - 사후 DB에서 `reward_claimed_at`이 NULL로 유지되고 포인트 변화 없음을 확인(트랜잭션 롤백 실증)
```
