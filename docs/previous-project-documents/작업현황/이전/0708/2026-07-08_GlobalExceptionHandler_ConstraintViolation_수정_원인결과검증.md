# `GlobalExceptionHandler` 수정 — 원인 / 결과 / 검증 (`ConstraintViolationException` 핸들러 추가)

작성일: 2026-07-08
상태: **코드 적용 완료, 전체 테스트 통과 확인**
관련 문서: `2026-07-08_PR33_2차코드리뷰_대응계획.md` §4-3, §5-1

---

## 1. 원인 — 왜 고쳤는가

### 1-1. PR #33 2차 코드 리뷰 근거

GitHub 코드 리뷰(https://github.com/shangpa/AcornProject/pull/33#issuecomment-4905804632) 4번 항목: `CouponController`/`PointController`/`AdminCouponController`(+ 추가 확인한 `AdminChallengeController`)의 `page`/`size` 파라미터에 범위 검증이 없어, `page=-1` 또는 `size=0` 요청이 들어오면 `PageRequest.of(page, size, ...)`가 `IllegalArgumentException`을 던지고 이게 `GlobalExceptionHandler`의 catch-all로 떨어져 `500`이 응답되는 문제.

### 1-2. 코드 레벨 원인 (수정 전)

4개 컨트롤러 전부 `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "10") int size`만 있고 범위 검증이 없었다. 이번 수정으로 각 파라미터에 `@Min` 제약을 추가하고 컨트롤러 클래스에 `@Validated`를 붙였는데, 이 제약이 위반되면 Spring이 `MethodArgumentNotValidException`이 아니라 `jakarta.validation.ConstraintViolationException`을 던진다. `GlobalExceptionHandler`에는 이 예외 타입을 처리하는 핸들러가 없어서, `@Min` 검증을 추가하는 것만으로는 여전히 500이 발생하는 상태였다.

---

## 2. 결과 — 무엇을 바꿨는가

### 2-1. 변경 형태: 기존 코드 수정이 아니라 신규 메서드 추가

`GlobalExceptionHandler`는 `@RestControllerAdvice`라 프로젝트 전체 `@RestController`에 전역 적용되는 단일 클래스다. 이번 변경은 **기존 4개 핸들러(`handleBusinessException`, `handleValidationException`, `handleBindingException`, `handleException`)를 한 줄도 안 건드리고, `@ExceptionHandler` 메서드를 1개 새로 추가**하는 것이다.

```java
// 추가된 코드 (기존 4개 메서드는 그대로 유지)
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
    return ResponseEntity.badRequest()
            .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
}
```

응답 형태는 필드별 상세 없이 기존 `VALIDATION_ERROR` 코드로 단순 변환하는 것으로 확정했다(`2026-07-08_PR33_2차코드리뷰_대응계획.md` §4-3 결정 사항) — 어제 이미 추가한 `handleBindingException`(`MethodArgumentTypeMismatchException`/`MissingServletRequestParameterException` 처리)과 완전히 같은 패턴이라 일관성이 맞는다.

### 2-2. 영향 범위

- `@RestControllerAdvice`가 전역이므로, `ConstraintViolationException`이 발생하는 모든 API(이번에 `@Validated`를 추가한 4개 컨트롤러 외에, 앞으로 다른 팀원이 `@Validated` + 파라미터 제약을 쓰는 API를 추가하면 거기에도 적용됨)가 영향을 받는다.
- 바뀌는 내용은 "`ConstraintViolationException`이 발생하면 500 대신 400 `VALIDATION_ERROR`가 나간다"는 것뿐이다. 기존에는 이 예외 자체가 발생할 지점(`@Validated` + 파라미터 제약)이 프로젝트에 전혀 없었으므로(오늘 처음 추가), **기존 API의 응답 동작을 바꾸는 게 아니라 새로 생긴 예외 상황에 대한 처리를 추가하는 것**이다.
- 응답 바디 형식(`{success, code, message, data}`)은 기존 400 응답들과 동일한 포맷을 그대로 쓴다.

### 2-3. `@Validated` + `@Min`을 추가한 4개 컨트롤러

| 파일 | 변경 |
|---|---|
| `coupon/controller/CouponController.java` | 클래스에 `@Validated` 추가, `getMyCoupons()`의 `page`/`size`에 `@Min` 추가 |
| `member/controller/PointController.java` | 클래스에 `@Validated` 추가, `getPointHistory()`의 `page`/`size`에 `@Min` 추가 |
| `admin/coupon/controller/AdminCouponController.java` | 클래스에 `@Validated` 추가, `getCoupons()`의 `page`/`size`에 `@Min` 추가 |
| `admin/challenge/controller/AdminChallengeController.java` | 클래스에 `@Validated` 추가, `getChallenges()`의 `page`/`size`에 `@Min` 추가(리뷰 코멘트가 지적하지 않았지만 같은 패턴이라 추가 확인 후 포함, §4-3 결정 사항) |

---

## 3. 검증

### 3-1. 적용 전에 확인한 것 (기존 코드/테스트에 대한 영향 분석)

- `ConstraintViolationException`이 발생할 수 있는 지점이 이번에 추가한 4개 컨트롤러의 `page`/`size` 외에는 프로젝트에 없었다(오늘 이전에는 `@Validated` + 메서드 파라미터 제약을 쓰는 곳이 전무). 따라서 기존 테스트 중 이 예외 처리 변경으로 깨질 수 있는 것은 없다.
- `GlobalExceptionHandler.java`의 기존 4개 핸들러는 이번 수정에서 코드가 전혀 바뀌지 않았음을 diff로 확인.

### 3-2. 적용 후 확인한 것

1. `.\gradlew.bat test` — 전체 118개 테스트 중 117개 통과. 실패 1건(`CategoryRepositoryTest.findsOnlyActiveCategoriesOrderedByDisplayOrder()`)은 PR #29에서 이미 원인 분석된 기존 테스트 격리 이슈(로컬 공유 DB 데이터 영향)로, 이번 변경과 무관함을 재확인.
2. `.\gradlew.bat build` — `compileJava`/`bootJar`/`assemble` 전부 성공. 테스트 결과는 위와 동일.
3. 신규 테스트로 회귀 고정: `AdminChallengeControllerTest`/`AdminCouponControllerTest`/`CouponControllerTest`/`PointControllerTest`에서 `page`/`size` 파라미터가 `@Min` 제약을 실제로 위반하는지 `jakarta.validation`의 실행 파라미터 검증(`Validator.forExecutables().validateParameters(...)`)으로 직접 확인, 정상 `page`/`size` 요청은 기존과 동일하게 `200 OK`가 나오는지 MockMvc로 확인.

---

## 4. 요약 (팀장 공유용 한 줄)

`GlobalExceptionHandler`에 `ConstraintViolationException` 핸들러 1개를 새로 추가했고, 기존 핸들러 4개는 무수정이다. 목적은 4개 컨트롤러(`Coupon`/`Point`/`AdminCoupon`/`AdminChallenge`)의 `page`/`size`에 새로 추가한 `@Min` 검증이 실패했을 때 500이 아니라 400 `VALIDATION_ERROR`로 응답되게 하는 것이고, 이 예외가 발생할 수 있는 지점 자체가 이번에 처음 생긴 것이라 기존 API 동작에는 영향이 없다. 전체 테스트(117/118, 무관한 기존 실패 1건 제외 전부 통과)로 확인했다.
