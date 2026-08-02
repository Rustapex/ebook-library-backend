# PR #22 BaseEntity 규칙 점검 및 수정 계획

## 1. 작업 범위

- 대상 PR: https://github.com/shangpa/AcornProject/pull/22
- 확인 방법: `gh pr diff 22` 기반 diff 확인
- 공통 규칙 근거: `docs/공통문서/백엔드_공통_개발_규칙.md` 274-310행
- 이번 문서는 수정 계획만 작성한다. 실제 Java, SQL, Gradle, 테스트 파일은 수정하지 않는다.

## 2. 제외한 로컬 변경 파일

요청에 따라 `git status`에 잡힌 파일은 이번 판단과 수정 계획의 직접 검토 대상에서 제외했다.

- `src/main/java/com/acorn/abc/common/exception/ErrorCode.java`
- `src/main/java/com/acorn/abc/member/domain/Attendance.java`
- `src/main/java/com/acorn/abc/member/domain/Member.java`
- `src/main/java/com/acorn/abc/member/domain/PointHistory.java`
- `src/main/java/com/acorn/abc/member/dto/AttendanceCheckResponse.java`
- `src/main/java/com/acorn/abc/member/dto/AttendanceMonthResponse.java`
- `src/main/java/com/acorn/abc/member/dto/IssuedCouponResponse.java`
- `src/main/java/com/acorn/abc/member/dto/PointHistoryResponse.java`
- `src/main/java/com/acorn/abc/member/dto/PointSummaryResponse.java`
- `src/main/java/com/acorn/abc/member/repository/AttendanceRepository.java`
- `src/main/java/com/acorn/abc/member/repository/PointHistoryRepository.java`

## 3. 공통 규칙 요약

`docs/공통문서/백엔드_공통_개발_규칙.md` 274-310행 기준:

- 모든 Entity는 `BaseEntity`를 상속받아야 한다.
- `createdAt`, `updatedAt`은 모든 Entity에 `BaseEntity`로만 추가한다.
- Entity 내부에 `createdAt`, `updatedAt`을 직접 선언하지 않는다.
- `@EnableJpaAuditing`은 메인 클래스 또는 별도 Config에 선언되어야 한다.

## 4. 확인된 BaseEntity 규칙 불만족 지점

### 4.1 `BookRental`

- PR diff 위치: `src/main/java/com/acorn/abc/rental/domain/BookRental.java`
- 확인 내용:
  - diff hunk에 `public class BookRental {` 형태로 표시되어 `BaseEntity` 상속이 없다.
  - `@Column(name = "created_at", nullable = false) private LocalDateTime createdAt;` 직접 선언이 남아 있다.
- 불만족 규칙:
  - 모든 Entity는 `BaseEntity`를 상속해야 한다.
  - `createdAt`은 Entity에서 직접 선언하지 않아야 한다.

수정 계획:

1. `BookRental`이 실제 JPA Entity인지 기존 어노테이션을 확인한다.
2. `BookRental extends BaseEntity`로 변경한다.
3. 직접 선언된 `createdAt` 필드를 제거한다.
4. `createdAt` 제거 후 불필요해지는 import만 정리한다.
5. `startReadingIfNotStarted()`의 `rentalStartAt = LocalDateTime.now()`는 대여 시작 시각 도메인 값이므로 유지한다.
6. `BookRental`을 사용하는 테스트와 서비스에서 `getCreatedAt()` 호출이 있다면 `BaseEntity` 상속 getter로 정상 동작하는지 확인한다.

### 4.2 `Bookmark`

- PR diff 위치: `src/main/java/com/acorn/abc/rental/domain/Bookmark.java`
- 확인 내용:
  - diff hunk에 `public class Bookmark {` 형태로 표시되어 `BaseEntity` 상속이 없다.
  - `@Column(name = "created_at", nullable = false) private LocalDateTime createdAt;` 직접 선언이 남아 있다.
  - `Bookmark.create()`에서 `bookmark.createdAt = LocalDateTime.now();`로 생성 시각을 직접 대입한다.
- 불만족 규칙:
  - 모든 Entity는 `BaseEntity`를 상속해야 한다.
  - `createdAt`은 Entity에서 직접 선언하거나 직접 대입하지 않아야 한다.

수정 계획:

1. `Bookmark extends BaseEntity`로 변경한다.
2. 직접 선언된 `createdAt` 필드를 제거한다.
3. `Bookmark.create()`에서 `bookmark.createdAt = LocalDateTime.now();` 대입을 제거한다.
4. `BookmarkService`가 저장 직후 `bookmark.getCreatedAt()`을 응답에 사용하므로, `saveAndFlush()` 후 Auditing 값이 채워지는지 확인한다.
5. `BookmarkRepositoryTest`에서 생성 시각 검증이 있다면 Auditing 기준으로 기대값을 조정한다.

### 4.3 `ReadingProgress`

- PR diff 위치: `src/main/java/com/acorn/abc/rental/domain/ReadingProgress.java`
- 확인 내용:
  - diff hunk에 `public class ReadingProgress {` 형태로 표시되어 `BaseEntity` 상속이 없다.
  - 해당 diff 범위에서는 `completedAt`만 확인되며, `createdAt` 직접 선언 여부는 추가 확인이 필요하다.
- 불만족 규칙:
  - 모든 Entity는 `BaseEntity`를 상속해야 한다.

수정 계획:

1. `ReadingProgress` 전체 Entity 필드를 확인한다.
2. `ReadingProgress extends BaseEntity`로 변경한다.
3. 만약 `createdAt` 또는 `updatedAt` 직접 선언이 있으면 제거한다.
4. `completedAt`, `lastReadAt`은 도메인 이벤트 시각이므로 BaseEntity 공통 필드와 분리해서 유지한다.
5. 진행 저장/완독 테스트에서 `completedAt` 최초 1회 처리 로직이 유지되는지 확인한다.

## 5. 스키마 확인 및 보정 계획

BaseEntity를 적용하면 Entity 매핑상 공통 컬럼 `created_at`, `updated_at`이 필요하다. PR diff에서는 `src/main/resources/schema.sql`의 `bookmark` 테이블에 `created_at DATETIME NOT NULL`만 보이고 `updated_at`은 확인되지 않았다.

수정 계획:

1. 실제 수정 단계에서 `schema.sql`의 `book_rental`, `bookmark`, `reading_progress` 테이블 정의를 확인한다.
2. BaseEntity가 매핑하는 컬럼명과 DB 컬럼명이 일치하는지 확인한다.
3. 필요한 경우 세 테이블에 `updated_at` 컬럼을 추가한다.
4. 기존 `created_at` 컬럼은 BaseEntity의 `createdAt`과 매핑되도록 유지한다.
5. native SQL 테스트 데이터가 `created_at`만 넣고 있다면, `updated_at`이 `NOT NULL`인지 여부에 맞춰 테스트 insert SQL도 함께 조정한다.

## 6. BaseEntity 외 추가 확인된 정리 대상

### 6.1 `schema.sql` 불필요한 빈 줄

- PR diff 위치: `src/main/resources/schema.sql`
- 확인 내용:
  - `bookmark` 테이블의 `created_at DATETIME NOT NULL,` 아래에 빈 줄 여러 개가 추가되어 있다.
- 영향:
  - 기능 오류는 아니지만 schema diff 품질이 낮아 보인다.

수정 계획:

1. `bookmark` 테이블 정의의 불필요한 빈 줄을 제거한다.
2. 컬럼과 제약조건 사이에는 필요한 경우 빈 줄 1개 이하만 유지한다.

### 6.2 테스트 DB 전환은 이번 계획의 문제로 분류하지 않음

- PR diff에서 H2 test profile 제거, `@AutoConfigureTestDatabase(replace = NONE)` 전환, `application-test.properties` 삭제가 확인된다.
- 이 항목은 별도 팀 정책으로 확정된 테스트 DB 전략이므로 이번 BaseEntity 수정 계획의 문제로 분류하지 않는다.

## 7. 권장 수정 순서

1. `BookRental`, `Bookmark`, `ReadingProgress`의 현재 Entity 선언과 import를 확인한다.
2. 세 Entity에 `BaseEntity` 상속을 적용한다.
3. 직접 선언된 `createdAt`, `updatedAt` 필드를 제거한다.
4. `Bookmark.create()`의 `createdAt` 직접 대입을 제거한다.
5. `schema.sql`에서 세 테이블의 `created_at`, `updated_at` 컬럼 상태를 확인하고 BaseEntity 매핑과 맞춘다.
6. repository 테스트의 native insert SQL을 스키마 변경에 맞게 조정한다.
7. `schema.sql`의 불필요한 빈 줄을 정리한다.
8. `.\gradlew.bat test`로 회귀를 확인한다.

## 8. 완료 기준

- `BookRental`, `Bookmark`, `ReadingProgress`가 모두 `BaseEntity`를 상속한다.
- 세 Entity에 `createdAt`, `updatedAt` 직접 선언이 없다.
- 생성 시각은 JPA Auditing으로 채워진다.
- 도메인 시각인 `rentalStartAt`, `lastReadAt`, `completedAt`은 기존 의미대로 유지된다.
- schema와 Entity 매핑이 일치한다.
- 전체 테스트가 통과한다.

