# `GlobalExceptionHandler` 수정 — 원인 / 결과 / 검증

작성일: 2026-07-07
상태: **코드 수정 예정, 아직 미적용** (`git status` 확인 완료 — `GlobalExceptionHandler.java` 변경 없음)
관련 문서: `2026-07-07_챌린지출석포인트쿠폰API_Swagger검증_코드수정계획.md` 1절

---

## 1. 원인 — 왜 고치려고 하는가

### 1-1. 실제로 재현된 증거

챌린지/출석/포인트/쿠폰 API Swagger 검증 중 `API-ATTEND-002`(`GET /api/v1/me/attendance?yearMonth=`)에 형식이 잘못된 `yearMonth`(예: `2026/07`)를 넣었을 때, 저장된 실제 응답이 아래와 같다.

```json
{"code": "INTERNAL_SERVER_ERROR", "success": false, "message": "서버 내부 오류가 발생했습니다."}
```
(증거 파일: `api-test-results/챌린지출석포인트쿠폰 API/swagger/API-ATTEND-002 GET/yearMonth 형식 오류/500.json`)

`docs/공통문서/백엔드_공통_개발_규칙.md` 4절 기준으로는 이런 "입력값 오류"는 `400 Bad Request` + `VALIDATION_ERROR`로 응답해야 하는데, 실제로는 `500 Internal Server Error`가 나가고 있어 규칙 위반이다.

### 1-2. 코드 레벨 원인

`src/main/java/com/acorn/abc/common/exception/GlobalExceptionHandler.java`(전문, 현재 상태):

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) { ... }

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) { ... }

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception e) { ... }  // 그 외 전부 500
```

`@RequestParam`으로 받는 값의 타입이 안 맞거나(`yearMonth=2026/07`처럼 `YearMonth` 파싱 실패 → `MethodArgumentTypeMismatchException`), 필수 파라미터가 아예 없을 때(`MissingServletRequestParameterException`)를 처리하는 핸들러가 없다. 그래서 이 두 예외는 맨 아래 catch-all `handleException(Exception e)`로 떨어져 무조건 500이 된다.

---

## 2. 결과 — 무엇을 바꾸는가 (계획, 아직 미적용)

### 2-1. 변경 형태: 기존 코드 수정이 아니라 신규 메서드 추가

`GlobalExceptionHandler`는 `@RestControllerAdvice`라 프로젝트 전체 `@RestController`에 전역 적용되는 단일 클래스다(기능별로 나뉘어 있지 않음). 이번 변경은 **기존 3개 핸들러(`handleBusinessException`, `handleValidationException`, `handleException`)를 한 줄도 안 건드리고, `@ExceptionHandler` 메서드를 1개 새로 추가**하는 것이다.

```java
// 추가 예정 (기존 3개 메서드는 그대로 유지)
@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
public ResponseEntity<ErrorResponse> handleBindingException(Exception e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
}
```

Spring은 발생한 예외 타입에 가장 구체적으로 일치하는 핸들러를 실행한다. 지금은 이 두 예외 타입에 맞는 핸들러가 없어 catch-all(`Exception.class`, →500)로 흘러가는데, 새 핸들러가 추가되면 이 두 예외만 먼저 가로채 400으로 응답하고, **다른 모든 예외의 처리 경로(`BusinessException`, `@Valid` 검증 실패, 그 외 미분류 예외)는 지금과 완전히 동일하게 유지**된다.

### 2-2. 영향 범위

- `@RestControllerAdvice`가 전역이므로, 이 두 예외가 발생하는 모든 API(내 챌린지/출석/포인트/쿠폰 API뿐 아니라 다른 팀원이 만든 `@RequestParam` 기반 조회 API 전부)가 영향을 받는다.
- 바뀌는 내용은 "이 두 예외 상황의 응답이 `500`에서 `400 VALIDATION_ERROR`로 바뀐다"는 것뿐이다. 500은 애초에 규칙.md 4절 기준으로 의도된 응답이 아니었으므로, 이 변경은 사실상 **프로젝트 전체에 걸친 버그 픽스**이지 새로운 제약을 추가하는 게 아니다.
- 응답 바디 형식(`{success, code, message, data}`)은 기존 400 응답들과 동일한 포맷을 그대로 쓴다.

---

## 3. 검증

### 3-1. 적용 전에 이미 확인한 것 (기존 코드/테스트에 대한 영향 분석)

- `src/test/java` 전체를 `INTERNAL_SERVER_ERROR`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` 키워드로 검색 → **일치하는 테스트 없음.** 즉 이 변경으로 깨지는 기존 테스트는 현재 코드베이스 기준으로 없다.
- `GlobalExceptionHandler.java`가 이번 세션에서 아직 수정되지 않았음을 `git status`로 재확인(위 "상태" 참고).

### 3-2. 적용 후 확인해야 할 것 (실행 예정)

1. `.\gradlew.bat build` — 전체 컴파일 + 기존 테스트(85/86, 기존에 알려진 무관 이슈 1건 제외) 통과 여부 재확인.
2. Swagger 재실행: `GET /api/v1/me/attendance?yearMonth=2026/07` → `400 VALIDATION_ERROR` 확인, `GET /api/v1/me/attendance`(파라미터 누락) → `400 VALIDATION_ERROR` 확인(체크리스트 문서 3-1절 #2/#3).
3. 기존에 정상 동작하던 `BusinessException` 기반 API(예: `API-ATTEND-001` 중복 체크인 → `409 ATTENDANCE_ALREADY_EXISTS`) 및 `@Valid` 검증 실패 API(예: `API-ADMIN-MEMBER-004` description 누락 → `400 VALIDATION_ERROR`)가 이번 수정 후에도 기존과 동일하게 응답하는지 회귀 확인.

---

## 4. 요약 (팀장 공유용 한 줄)

`GlobalExceptionHandler`에 예외 핸들러 1개를 새로 추가하는 것뿐이며, 기존 핸들러 3개는 무수정이다. 목적은 쿼리 파라미터 형식 오류/누락 시 잘못 나가고 있던 `500`을 규칙.md 기준의 `400 VALIDATION_ERROR`로 바로잡는 것이고, 영향은 전역(다른 API에도 적용)이지만 기존 테스트에 영향 없음을 확인했다.
