# Entity 기준 schema.sql created_at/updated_at 규칙 위반 검토 보고

## 1. 검토 목적

- 이미 생성된 Java `@Entity`에 대응하는 `src/main/resources/schema.sql`의 `CREATE TABLE` 쿼리만 대상으로 `created_at`, `updated_at` 누락 여부를 확인한다.
- `docs/공통문서/백엔드_공통_개발_규칙.md`를 1순위 규칙, `docs/API 명세/api-spec(final).md`를 2순위 규칙으로 적용한다.
- 이번 보고서는 검사 결과 보고서이며, `schema.sql` 또는 Java Entity 파일은 수정하지 않았다.

## 2. 검토 기준

### 2.1 1순위: 백엔드 공통 개발 규칙

근거:

- `docs/공통문서/백엔드_공통_개발_규칙.md:274` - BaseEntity 규칙
- `docs/공통문서/백엔드_공통_개발_규칙.md:276` - 모든 Entity는 `BaseEntity`를 상속해 공통 필드를 사용
- `docs/공통문서/백엔드_공통_개발_규칙.md:284` - `createdAt`
- `docs/공통문서/백엔드_공통_개발_규칙.md:288` - `updatedAt`
- `docs/공통문서/백엔드_공통_개발_규칙.md:309` - `createdAt`, `updatedAt`은 모든 Entity에 BaseEntity로만 추가하고 직접 선언하지 않음

판정 기준:

- 실제 Java `@Entity`가 존재하는 테이블은 최종적으로 `BaseEntity` 공통 필드와 매핑될 수 있어야 하므로 `schema.sql`의 해당 `CREATE TABLE`에 `created_at`, `updated_at`이 모두 있어야 한다.
- 둘 중 하나라도 없으면 `schema.sql` 쿼리 위반으로 분류했다.

### 2.2 2순위: API 명세

근거:

- `docs/API 명세/api-spec(final).md:157` - 공통 필드로 `createdAt` 정의
- `docs/API 명세/api-spec(final).md:173`-`180` - 사용자 도서/카테고리/컬렉션/도서관 API 관련 Entity
- `docs/API 명세/api-spec(final).md:214`-`219` - 관리자 도서/카테고리 API 관련 Entity
- `docs/API 명세/api-spec(final).md:238`-`244` - 관리자 도서관/컬렉션 API 관련 Entity
- `docs/API 명세/api-spec(final).md:333` - 관리자 도서 등록 응답 `createdAt`
- `docs/API 명세/api-spec(final).md:334` - 관리자 도서 수정 응답 `updatedAt`

API 명세는 특정 응답 필드와 관련 Entity를 보강 근거로 사용했다. 공통 규칙과 상충되는 부분은 확인되지 않았고, 감사 시각 컬럼 필요성은 공통 규칙을 우선 적용했다.

## 3. 검토 방법

- Java Entity 검색: `@Entity`, `@Table(name = ...)`, `createdAt`, `updatedAt`, `extends BaseEntity`
- SQL 검색: `CREATE TABLE IF NOT EXISTS`, `created_at`, `updated_at`
- `schema.sql`은 전체 본문을 인용하지 않고, Entity에 대응하는 테이블의 `CREATE TABLE` 시작 라인과 감사 컬럼 존재 여부만 비교했다.

## 4. 전체 결과

| 항목 | 결과 |
|---|---:|
| Java `@Entity` 수 | 43 |
| `schema.sql`에 대응 테이블이 있는 Entity | 43 |
| `created_at`, `updated_at` 둘 다 있는 테이블 | 9 |
| `created_at` 또는 `updated_at` 누락 테이블 | 34 |
| `created_at`, `updated_at` 둘 다 누락 | 12 |
| `created_at`만 누락 | 3 |
| `updated_at`만 누락 | 19 |

현재 코드 기준으로 `BaseEntity` 클래스, `@MappedSuperclass`, `@CreatedDate`, `@LastModifiedDate`, `@EnableJpaAuditing`, `extends BaseEntity`는 검색되지 않았다. 이 항목은 Java 코드 쪽 규칙 위반 참고 사항이며, 아래 표는 사용자가 요청한 `schema.sql` 쿼리 위반만 정리한다.

## 5. schema.sql 쿼리 위반 목록

| 테이블 | Entity | 누락 컬럼 | Entity 근거 | SQL 근거 |
|---|---|---|---|---|
| `admin_audit_log` | `AdminAuditLog` | `updated_at` | `src/main/java/com/acorn/abc/statistic/domain/AdminAuditLog.java:20` | `src/main/resources/schema.sql:570` |
| `admin_statistic` | `AdminStatistic` | `created_at` | `src/main/java/com/acorn/abc/statistic/domain/AdminStatistic.java:17` | `src/main/resources/schema.sql:547` |
| `attendance` | `Attendance` | `updated_at` | `src/main/java/com/acorn/abc/member/domain/Attendance.java:20` | `src/main/resources/schema.sql:183` |
| `author` | `Author` | `updated_at` | `src/main/java/com/acorn/abc/book/domain/Author.java:16` | `src/main/resources/schema.sql:55` |
| `book_author` | `BookAuthor` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/book/domain/BookAuthor.java:16` | `src/main/resources/schema.sql:227` |
| `book_category` | `BookCategory` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/book/domain/BookCategory.java:15` | `src/main/resources/schema.sql:258` |
| `book_keyword` | `BookKeyword` | `updated_at` | `src/main/java/com/acorn/abc/book/domain/BookKeyword.java:16` | `src/main/resources/schema.sql:76` |
| `book_keyword_map` | `BookKeywordMap` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/book/domain/BookKeywordMap.java:16` | `src/main/resources/schema.sql:264` |
| `book_rental` | `BookRental` | `updated_at` | `src/main/java/com/acorn/abc/rental/domain/BookRental.java:21` | `src/main/resources/schema.sql:271` |
| `book_report` | `BookReport` | `updated_at` | `src/main/java/com/acorn/abc/review/domain/BookReport.java:21` | `src/main/resources/schema.sql:337` |
| `book_request` | `BookRequest` | `updated_at` | `src/main/java/com/acorn/abc/request/domain/BookRequest.java:21` | `src/main/resources/schema.sql:445` |
| `book_request_candidate` | `BookRequestCandidate` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/request/domain/BookRequestCandidate.java:16` | `src/main/resources/schema.sql:115` |
| `bookmark` | `Bookmark` | `updated_at` | `src/main/java/com/acorn/abc/rental/domain/Bookmark.java:19` | `src/main/resources/schema.sql:295` |
| `category` | `Category` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/book/domain/Category.java:17` | `src/main/resources/schema.sql:66` |
| `challenge` | `Challenge` | `updated_at` | `src/main/java/com/acorn/abc/challenge/domain/Challenge.java:16` | `src/main/resources/schema.sql:102` |
| `challenge_progress` | `ChallengeProgress` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/challenge/domain/ChallengeProgress.java:21` | `src/main/resources/schema.sql:421` |
| `challenge_reward` | `ChallengeReward` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/challenge/domain/ChallengeReward.java:18` | `src/main/resources/schema.sql:434` |
| `collection_book` | `CollectionBook` | `updated_at` | `src/main/java/com/acorn/abc/collection/domain/CollectionBook.java:20` | `src/main/resources/schema.sql:494` |
| `coupon` | `Coupon` | `updated_at` | `src/main/java/com/acorn/abc/payment/domain/Coupon.java:16` | `src/main/resources/schema.sql:88` |
| `coupon_usage` | `CouponUsage` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/payment/domain/CouponUsage.java:20` | `src/main/resources/schema.sql:408` |
| `favorite_book` | `FavoriteBook` | `updated_at` | `src/main/java/com/acorn/abc/favorite/domain/FavoriteBook.java:21` | `src/main/resources/schema.sql:304` |
| `library` | `Library` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/library/domain/Library.java:16` | `src/main/resources/schema.sql:127` |
| `library_book` | `LibraryBook` | `created_at` | `src/main/java/com/acorn/abc/library/domain/LibraryBook.java:20` | `src/main/resources/schema.sql:466` |
| `member_coupon` | `MemberCoupon` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/payment/domain/MemberCoupon.java:20` | `src/main/resources/schema.sql:396` |
| `member_grade` | `MemberGrade` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/member/domain/MemberGrade.java:16` | `src/main/resources/schema.sql:31` |
| `notification` | `Notification` | `updated_at` | `src/main/java/com/acorn/abc/notification/domain/Notification.java:20` | `src/main/resources/schema.sql:504` |
| `payment` | `Payment` | `updated_at` | `src/main/java/com/acorn/abc/payment/domain/Payment.java:21` | `src/main/resources/schema.sql:378` |
| `point_history` | `PointHistory` | `updated_at` | `src/main/java/com/acorn/abc/member/domain/PointHistory.java:20` | `src/main/resources/schema.sql:192` |
| `publisher` | `Publisher` | `updated_at` | `src/main/java/com/acorn/abc/book/domain/Publisher.java:16` | `src/main/resources/schema.sql:44` |
| `rating` | `Rating` | `updated_at` | `src/main/java/com/acorn/abc/review/domain/Rating.java:21` | `src/main/resources/schema.sql:326` |
| `reading_progress` | `ReadingProgress` | `created_at`, `updated_at` | `src/main/java/com/acorn/abc/rental/domain/ReadingProgress.java:19` | `src/main/resources/schema.sql:283` |
| `review_report` | `ReviewReport` | `updated_at` | `src/main/java/com/acorn/abc/review/domain/ReviewReport.java:20` | `src/main/resources/schema.sql:351` |
| `sanction_history` | `SanctionHistory` | `updated_at` | `src/main/java/com/acorn/abc/review/domain/SanctionHistory.java:20` | `src/main/resources/schema.sql:365` |
| `user_reading_statistic` | `UserReadingStatistic` | `created_at` | `src/main/java/com/acorn/abc/statistic/domain/UserReadingStatistic.java:21` | `src/main/resources/schema.sql:530` |

## 6. 카탈로그 API와 직접 관련된 우선 확인 대상

API 명세의 도서/카테고리/컬렉션/도서관 영역과 직접 연결되는 위반 테이블은 다음이다.

| 우선순위 | 테이블 | 이유 |
|---:|---|---|
| 1 | `category` | 사용자/관리자 카테고리 API의 직접 Entity이고 등록/수정 대상인데 `created_at`, `updated_at`이 모두 없다. |
| 2 | `book_author` | 관리자 도서 등록/수정 API에서 도서-저자 매핑을 변경하는데 감사 컬럼이 모두 없다. |
| 3 | `book_category` | 도서 목록 필터, 카테고리 관리, 관리자 도서 등록/수정에 연결되는데 감사 컬럼이 모두 없다. |
| 4 | `book_keyword_map` | 추천/검색/도서 상세 키워드 매핑에 연결되는데 감사 컬럼이 모두 없다. |
| 5 | `library` | 도서관 등록/수정 API 대상인데 감사 컬럼이 모두 없다. |
| 6 | `library_book` | 도서관 보유 도서 변경 API 대상인데 `created_at`이 없다. |
| 7 | `publisher` | 관리자 도서 등록/수정에서 출판사명이 포함되며 `updated_at`이 없다. |
| 8 | `author` | 관리자 도서 등록/수정에서 저자 매핑 및 저자 정보가 사용되며 `updated_at`이 없다. |
| 9 | `book_keyword` | 도서 검색/추천 키워드 Entity인데 `updated_at`이 없다. |
| 10 | `collection_book` | 컬렉션 도서 추가/정렬 API 대상인데 `updated_at`이 없다. |

`book`, `book_detail`, `book_page`, `collection`은 현재 `schema.sql`에 `created_at`, `updated_at`이 모두 있어 스키마 쿼리 기준으로는 통과한다.

## 7. 수정 필요성

1순위 규칙을 적용하면 모든 Entity는 `BaseEntity`의 `createdAt`, `updatedAt`을 사용해야 한다. 따라서 해당 Entity의 실제 테이블에는 `created_at`, `updated_at` 컬럼이 존재해야 한다.

현재 위반 테이블을 그대로 두고 Java Entity를 BaseEntity 기반으로 고치면, JPA가 `created_at`, `updated_at` 컬럼을 매핑하려는 시점에 테이블 컬럼 누락 문제가 발생할 수 있다. 반대로 Java Entity를 고치지 않더라도, 공통 규칙 기준으로는 현재 `schema.sql` 쿼리가 이미 일관성을 잃은 상태다.

권장 수정 방향:

- 위반 테이블의 `CREATE TABLE`에 `created_at DATETIME NOT NULL`, `updated_at DATETIME NULL`을 추가한다.
- Java Entity에서는 직접 선언된 `createdAt`, `updatedAt`을 제거하고 `BaseEntity` 상속으로 통일한다.
- `@EnableJpaAuditing` 설정을 추가하거나 기존 설정 존재 여부를 별도 확인한다.
- 백엔드 코드를 실제 수정하는 경우, 최종 보고 전 `.\gradlew.bat archTest`를 실행해 공통 규칙 위반 0을 확인한다.

