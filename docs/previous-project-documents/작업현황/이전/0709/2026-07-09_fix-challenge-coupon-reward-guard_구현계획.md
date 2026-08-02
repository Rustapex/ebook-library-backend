# `fix/challenge-coupon-reward-guard` 브랜치 구현 계획

- 브랜치: `fix/challenge-coupon-reward-guard` (main에서 분기, 커밋 `94f7228` 기준)
- 근거 문서: `docs/작업현황/2026-07-09_PR33_5차리뷰_및_후속작업_대응계획.md` §3(coupon fail-fast), §6(결정 사항), §7(브랜치 A)
- 작성일: 2026-07-09
- 상태: **계획만, 코드 미수정.** 이 문서는 근거 문서의 §7 "브랜치 A" 부분을 실행 체크리스트 수준으로 구체화한 것.

---

## 0. 이 브랜치에서 할 일 (범위 확정)

1. `ChallengeService.claimReward()`에서 `POINT`가 아닌 reward 타입을 만나면 fail-fast(`BusinessException`)
2. `ChallengeProgressRepository.markRewardClaimedIfUnclaimed()` 순차 호출 검증 테스트 추가
3. `GlobalExceptionHandler` 1차 핸들러(`MethodArgumentTypeMismatchException`/`MissingServletRequestParameterException`) 회귀 테스트 추가

**이 브랜치에서 하지 않는 것**(근거 문서 §6 참고): BaseEntity 미상속 정리, PR #33 본문 §8 정리, `usableForBookId`(문제 아님으로 종결), 쿠폰 실제 지급 로직 구현(별도 세션/브랜치).

---

## 1. `ChallengeService.claimReward()` fail-fast 추가

### 1-1. 현재 코드 (수정 대상, `ChallengeService.java:116-124`)

```java
for (ChallengeReward reward : rewards) {
    if ("POINT".equals(reward.getRewardType()) && reward.getRewardPoint() != null) {
        int amount = reward.getRewardPoint() * reward.getRewardQuantity();
        pointService.grantOrAdjustPoint(memberId, amount, "challenge_reward",
                challenge.getChallengeName() + " 챌린지 보상");
        grantedPoint += amount;
    }
    // TODO: rewardType=coupon 지급 연동은 이번 범위에서 보류(계획 문서 참고, 쿠폰 발급 로직은 관리자 발급 경로 완성 후 연동)
}
```

### 1-2. 수정 방안

- `if` 블록에 `else`를 추가해 `POINT`가 아닌 타입을 만나면 예외를 던지도록 변경.
- `ErrorCode`에 신규 상수 추가: `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED(HttpStatus.CONFLICT, "CHALLENGE_REWARD_TYPE_NOT_SUPPORTED", "아직 지원하지 않는 보상 유형입니다.")` — 기존 `CHALLENGE_REWARD_NOT_CLAIMABLE`과 동일하게 409로 처리(비즈니스 규칙상 "지금은 처리 불가" 성격).
- `GlobalExceptionHandler`는 **수정하지 않음**(기존 `BusinessException` 핸들러가 처리, 확인 완료).
- 안전성: `ChallengeService`는 클래스 레벨 `@Transactional`, `BusinessException`은 `RuntimeException` 상속 확인 완료 → `markRewardClaimedIfUnclaimed()`로 먼저 선점한 뒤 예외를 던져도 트랜잭션 전체가 롤백되어 선점이 취소됨(확인 완료, 근거 문서 §3-3 참고).

### 1-3. 영향받는 파일

- `src/main/java/com/acorn/abc/common/exception/ErrorCode.java` — 신규 상수 1개 추가
- `src/main/java/com/acorn/abc/challenge/service/ChallengeService.java` — `claimReward()` 루프에 `else` 분기 추가
- `src/test/java/com/acorn/abc/challenge/service/ChallengeServiceTest.java` — coupon(또는 그 외 미지원 타입) reward를 가진 챌린지에 대해 `claimReward()` 호출 시 `BusinessException`(`CHALLENGE_REWARD_TYPE_NOT_SUPPORTED`)이 발생하는지, 그리고 `markRewardClaimedIfUnclaimed()` 호출 이후 상태가 롤백되는지(트랜잭션 자체는 단위 테스트로 직접 검증 어려움 — `@Transactional` 롤백은 통합 테스트 영역이라, 단위 테스트에서는 "예외가 던져진다"까지만 확인하고 롤백은 근거 문서의 코드 분석으로 갈음)

---

## 2. `markRewardClaimedIfUnclaimed()` 리포지토리 테스트 추가

### 2-1. 목적 (오해 방지, 근거 문서 §6-6 참고)

**동시성 테스트가 아니다.** 같은 메서드를 순차적으로 두 번 호출해서 "1건 반영 → 0건 반영"이라는 SQL 정합성만 확인하는 일반 단위 테스트. 실제 동시 요청 시나리오는 기존처럼 Postman Collection Runner로 검증(변경 없음).

### 2-2. 테스트 위치 및 방안

- 파일: `src/test/java/com/acorn/abc/challenge/repository/ChallengeProgressRepositoryTest.java`(기존 파일, `@DataJpaTest` 패턴 — `2026-07-08_PR33_4차코드리뷰_대응계획.md` 작업 때 이미 이 파일에 batch 조회 테스트를 추가한 전례 있음, 같은 스타일 유지)
- 시나리오: progress row 하나를 삽입한 뒤 `markRewardClaimedIfUnclaimed(progressId, now)`를 첫 번째 호출 → 영향받은 row 수 1 확인. 같은 메서드를 즉시 재호출 → 영향받은 row 수 0 확인(이미 `reward_claimed_at`이 채워져 있어 `WHERE reward_claimed_at IS NULL` 조건에 안 걸림).

### 2-3. 영향받는 파일

- `src/test/java/com/acorn/abc/challenge/repository/ChallengeProgressRepositoryTest.java` — 테스트 2개(또는 1개 메서드 안에서 순차 assert) 추가

---

## 3. `GlobalExceptionHandler` 1차 핸들러 회귀 테스트

### 3-1. 대상 핸들러 (재확인, `GlobalExceptionHandler.java:39-43`)

```java
@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
public ResponseEntity<ErrorResponse> handleBindingException(Exception e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
}
```

### 3-2. 재현 방법 확인 (실행 시 재확인 필요 항목)

- **`MethodArgumentTypeMismatchException`**: 기존 컨트롤러(`AdminChallengeController` 등)의 `page`/`size`가 `int` 타입이라, `page=abc`처럼 숫자로 변환 불가능한 값을 넘기면 재현 가능(코드 확인 완료, `AdminChallengeController.java:38-39`).
- **`MissingServletRequestParameterException`**: 확인 결과 `page`/`size`는 `@RequestParam(defaultValue = ...)`라서 아예 안 넘겨도 기본값이 적용돼 이 예외가 발생하지 않음. **이 예외를 재현할 required(`defaultValue` 없는) `@RequestParam`을 가진 엔드포인트를 구현 시점에 다시 찾아야 함** — 없으면 이 타입은 이번 테스트 대상에서 제외하고 `MethodArgumentTypeMismatchException`만 커버.

### 3-3. 테스트 위치

- 기존 컨트롤러 테스트 파일 중 하나(예: `AdminChallengeControllerTest.java`)에 케이스 추가, 또는 `GlobalExceptionHandler` 전용 테스트 파일이 없다면 신규 생성 여부는 구현 시 결정.

---

## 4. 검증 방법

1. `.\gradlew.bat test --tests com.acorn.abc.challenge.service.ChallengeServiceTest` 단독 실행
2. `.\gradlew.bat test --tests com.acorn.abc.challenge.repository.ChallengeProgressRepositoryTest` 단독 실행
3. 3번(exception handler) 관련 테스트 파일 단독 실행
4. 전체 `.\gradlew.bat test` 실행 — 기존 `CategoryRepositoryTest` 1건 실패(무관 이슈)는 정상, 그 외 전부 통과 확인

---

## 5. 커밋/PR

- 커밋 메시지/PR 메시지 초안은 구현 완료 후 git-message-standardizer로 별도 작성(이 문서에는 포함하지 않음, 요청 시 별도 진행)
- PR base: `main`, head: `fix/challenge-coupon-reward-guard`
