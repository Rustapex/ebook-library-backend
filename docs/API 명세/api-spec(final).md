# ABC(Acorn Book Cloud) API 명세서 (Final)

> 수정일: 2026-06-17  
> 작성 기준: `final/01-공통-화면설계기준 (1).md`, `final/02-User-상세화면설계도 (2).md`, `final/03-Admin-상세화면설계도.md`, `final/04-전체-화면이동흐름.md`, `final/ebook-rental-erd (final).md`, 기존 예시 `api-spec.md`  
> 목적: final 화면설계와 final ERD 기준으로 기존 API 명세의 비밀번호 재설정, 반납/연체/환불, 사용자 신고 등 제외 기능을 제거하고 컬렉션·후보형 희망도서·최신 결제/리뷰/알림 정책을 반영한다.  
> 표기: URL/Method/Entity/컬럼명/JSON key는 English, 설명은 한국어.

---

## 1. 공통 API 규칙

### 1.1 Base URL

```text
/api/v1
```

### 1.2 인증 방식

| 구분 | 방식 |
|---|---|
| 비회원/공개 API | Authorization Header 없음 |
| 회원 API | `Authorization: Bearer {accessToken}` |
| 관리자 API | `Authorization: Bearer {accessToken}` + `MEMBER.role = ADMIN` |

- 로그인 성공 대상은 `MEMBER.status = JOINED`이다.
- `SANCTIONED`, `WITHDRAWN`, `DEACTIVATED` 계정은 로그인 또는 주요 활동 요청을 거부한다.
- 세부 제재(`RENTAL_BAN`, `REVIEW_BAN`, `ACCOUNT_SUSPENSION`)는 `SANCTION_HISTORY`의 유효 기간 기준으로 판단한다.

### 1.3 공통 응답 형식

#### 성공

```json
{
  "success": true,
  "data": {},
  "message": "success"
}
```

#### 실패

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지"
  }
}
```

### 1.4 공통 페이징 형식

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```

- `page`는 1부터 시작한다.
- 기본 `size`는 20, 화면 카드형 목록은 화면 요구에 맞춰 6/12/24 등을 사용할 수 있다.

### 1.5 공통 상태코드

| Status | 의미 |
|---:|---|
| 200 | 조회/수정/삭제 성공 |
| 201 | 생성 성공 |
| 400 | 요청 파라미터 또는 Body 검증 실패 |
| 401 | 인증 실패 또는 토큰 없음 |
| 403 | 권한 없음, 관리자 권한 없음, 제재로 인한 활동 제한 |
| 404 | 리소스 없음 |
| 409 | 중복, 이미 처리됨, 현재 상태와 충돌 |
| 422 | 비즈니스 규칙 위반 |
| 500 | 서버 오류 |

### 1.6 주요 Enum 및 정책

| 영역 | 허용 값/정책 |
|---|---|
| 회원 상태 | `JOINED`, `SANCTIONED`, `WITHDRAWN`, `DEACTIVATED` |
| 회원 권한 | `USER`, `ADMIN` |
| 도서 상태 | User 노출은 `AVAILABLE`만. Admin은 `AVAILABLE`, `HIDDEN`, `INACTIVE` 관리 |
| 기준정보 상태 | 출판사/저자/카테고리/쿠폰/챌린지는 `ACTIVE`, `INACTIVE` |
| 대여 상태 | `READY`, `READING`, `OWNED`만 사용. 반납/연체 상태 없음 |
| 결제 | `paymentType=RENTAL`, `paymentMethod=CARD`, `paymentStatus=PAID`만 저장 |
| 포인트 유형 | `CHALLENGE_REWARD`, `REVIEW_REWARD`, `PAYBACK`, `EVENT_REWARD`, `PAYMENT_USE`, `ADMIN_ADJUST` |
| 쿠폰 | `PERCENT_DISCOUNT`/`AMOUNT_DISCOUNT`, `benefitUnit=PERCENT/AMOUNT`, 회원 쿠폰 상태 `ISSUED/USED/EXPIRED` |
| 리뷰 | `REVIEW.status = ACTIVE/HIDDEN/DELETED`, 회원+도서 기준 1개 |
| 별점 | `RATING.ratingScore` 1~5, 회원+도서 기준 1개, 생성 후 수정/삭제 불가 |
| 신고 대상 | 책/리뷰만. 사용자 신고 API 없음 |
| 신고 상태 | `WAITING`, `PROCESSING`, `DONE`, `REJECTED` |
| 희망도서 | 후보 단위 `BOOK_REQUEST_CANDIDATE`, 신청 단위 `BOOK_REQUEST`, 상태 `REQUESTED/APPROVED/REJECTED` |
| 알림 유형 | `RENTAL`, `PAYMENT`, `NOTICE`, `EVENT`, `REPORT`, `BOOK_REQUEST`, `COUPON`, `CHALLENGE` |
| 컬렉션 | `SERIES`, `EVENT`; 상태 `ACTIVE`, `HIDDEN`, `ENDED` |

### 1.7 final 반영 핵심 변경점

| 기존 예시 항목 | final 반영 |
|---|---|
| 비밀번호 재설정 토큰/비회원 비밀번호 재설정 | 제외. 로그인 후 `API-ME-003`으로만 비밀번호 변경 |
| 대여 반납/연체/연체료 결제 | 제외. 대여 상태는 `READY`, `READING`, `OWNED`만 사용 |
| 결제 취소/환불/복구 | 제외. 완료된 `PAID` 결제 row만 저장 |
| 결제수단 `COUPON` | 제외. 쿠폰/포인트는 할인/차감 수단이며 결제수단은 `CARD`만 사용 |
| 리뷰 중복 기준 `rentalId` | 변경. `REVIEW`와 `RATING` 모두 회원+도서 기준 1개 |
| 사용자 신고 | 제외. 책/리뷰 신고만 제공 |
| 알림 본문 | 저장/반환하지 않음. 제목, 유형, 연결 대상, 읽음 여부, 일시만 사용 |
| 컬렉션 | 추가. 시리즈/이벤트는 `COLLECTION`, `COLLECTION_BOOK`으로 관리 |

---

## 2. 공통 객체 스키마

### 2.1 BookCard

| Field | Type | 설명 |
|---|---|---|
| `bookId` | number | 도서 번호 |
| `title` | string | 제목 |
| `coverImageUrl` | string | 표지 이미지 URL |
| `authors` | array | 저자명 목록 |
| `publisherName` | string | 출판사명 |
| `rentalType` | string | `FREE` 또는 `PAID` |
| `rentalPrice` | number | 유료 대여 가격 |
| `averageRating` | number | 평균 별점 |
| `reviewCount` | number | 활성 리뷰 수 |
| `favoriteYn` | boolean | 로그인 회원 기준 즐겨찾기 여부 |

### 2.2 PaymentAmount

| Field | Type | 설명 |
|---|---|---|
| `originalAmount` | number | 할인 전 금액 |
| `couponDiscountAmount` | number | 쿠폰 할인 금액 |
| `pointUsedAmount` | number | 포인트 차감 금액 |
| `amount` | number | 최종 카드 결제 금액. 0 가능 |
| `paymentMethod` | string | 항상 `CARD` |
| `paymentStatus` | string | 항상 `PAID` |

### 2.3 NotificationItem

| Field | Type | 설명 |
|---|---|---|
| `notificationId` | number | 알림 번호 |
| `notificationType` | string | 알림 유형 |
| `title` | string | 알림 제목 |
| `targetType` | string/null | 연결 대상 유형 |
| `targetId` | number/null | 연결 대상 ID |
| `readYn` | boolean | 읽음 여부 |
| `createdAt` | datetime | 생성일 |
| `readAt` | datetime/null | 읽은 시각 |

---

## 3. API 목록 요약

| API ID | API 이름 | Method | URL | 권한 | 화면 | 관련 Entity |
|---|---|---|---|---|---|---|
| API-AUTH-001 | 회원가입 | POST | `/api/v1/auth/signup` | 비회원 | U-002 | `MEMBER, MEMBER_GRADE` |
| API-AUTH-002 | 로그인 | POST | `/api/v1/auth/login` | 비회원 | U-003 | `MEMBER, MEMBER_GRADE, SANCTION_HISTORY, MEMBER_COUPON` |
| API-AUTH-003 | 아이디 찾기 | POST | `/api/v1/auth/find-id` | 비회원 | U-004 | `MEMBER` |
| API-ME-001 | 내 정보 조회 | GET | `/api/v1/me` | 회원 | U-014, U-015 | `MEMBER, MEMBER_GRADE, USER_READING_STATISTICS, MEMBER_COUPON, BOOK_RENTAL, FAVORITE_BOOK` |
| API-ME-002 | 회원정보 수정 | PATCH | `/api/v1/me` | 회원 | U-015 | `MEMBER` |
| API-ME-003 | 비밀번호 변경 | PATCH | `/api/v1/me/password` | 회원 | U-015 | `MEMBER` |
| API-ME-004 | 회원 탈퇴 | DELETE | `/api/v1/me` | 회원 | U-015 | `MEMBER, BOOK_RENTAL` |
| API-BOOK-001 | 도서 목록 조회 | GET | `/api/v1/books` | 비회원/회원 | U-001, U-006 | `BOOK, AUTHOR, PUBLISHER, CATEGORY, BOOK_CATEGORY, FAVORITE_BOOK` |
| API-BOOK-002 | 도서 검색 | GET | `/api/v1/books/search` | 비회원/회원 | U-007 | `BOOK, AUTHOR, PUBLISHER, CATEGORY, BOOK_KEYWORD` |
| API-BOOK-003 | 도서 상세 조회 | GET | `/api/v1/books/{bookId}` | 비회원/회원 | U-008 | `BOOK, BOOK_DETAIL, BOOK_FILE, AUTHOR, PUBLISHER, CATEGORY, BOOK_KEYWORD, REVIEW, RATING, LIBRARY_BOOK` |
| API-BOOK-005 | 추천 도서 조회 | GET | `/api/v1/books/recommendations` | 비회원/회원 | U-001, U-008, U-025 | `BOOK, AUTHOR, CATEGORY, BOOK_KEYWORD, BOOK_KEYWORD_MAP` |
| API-CATEGORY-001 | 카테고리 목록 조회 | GET | `/api/v1/categories` | 비회원/회원 | U-001, U-006 | `CATEGORY` |
| API-COLLECTION-001 | 컬렉션 목록 조회 | GET | `/api/v1/collections` | 비회원/회원 | U-001, U-006(EVENT target) | `COLLECTION, COLLECTION_BOOK, BOOK` |
| API-COLLECTION-002 | 컬렉션 상세/도서 조회 | GET | `/api/v1/collections/{collectionId}` | 비회원/회원 | U-001, U-006(EVENT target) | `COLLECTION, COLLECTION_BOOK, BOOK` |
| API-LIBRARY-001 | 도서 보유 도서관 목록 조회 | GET | `/api/v1/books/{bookId}/libraries` | 비회원/회원 | U-024 | `LIBRARY, LIBRARY_BOOK` |
| API-RENTAL-001 | 무료 대여 생성 | POST | `/api/v1/rentals/free` | 회원 | U-009 | `BOOK, BOOK_RENTAL, NOTIFICATION` |
| API-RENTAL-003 | 내 대여 현황 조회 | GET | `/api/v1/me/rentals` | 회원 | U-010, U-014 | `BOOK_RENTAL, BOOK, READING_PROGRESS` |
| API-READING-001 | 최근 읽은 책 조회 | GET | `/api/v1/me/recent-books` | 회원 | U-014, U-028 | `READING_PROGRESS, BOOK_RENTAL, BOOK` |
| API-PAYMENT-001 | 유료 대여 결제 및 대여 생성 | POST | `/api/v1/rentals/paid` | 회원 | U-009 | `BOOK, BOOK_RENTAL, PAYMENT, MEMBER_COUPON, COUPON_USAGE, POINT_HISTORY, MEMBER, NOTIFICATION` |
| API-PAYMENT-003 | 내 결제 내역 조회 | GET | `/api/v1/me/payments` | 회원 | U-018, U-014 | `PAYMENT, BOOK_RENTAL, BOOK, COUPON_USAGE, POINT_HISTORY` |
| API-VIEWER-001 | 뷰어 페이지 조회 | GET | `/api/v1/rentals/{rentalId}/pages/{pageNo}` | 회원 | U-011 | `BOOK_RENTAL, BOOK_PAGE, BOOK_FILE, READING_PROGRESS, BOOKMARK` |
| API-VIEWER-002 | 읽기 진행 저장 | PUT | `/api/v1/rentals/{rentalId}/progress` | 회원 | U-011 | `READING_PROGRESS, BOOK_RENTAL, USER_READING_STATISTICS, ADMIN_STATISTICS` |
| API-VIEWER-003 | 북마크 추가 | POST | `/api/v1/rentals/{rentalId}/bookmarks` | 회원 | U-011 | `BOOKMARK` |
| API-VIEWER-004 | 북마크 삭제 | DELETE | `/api/v1/rentals/{rentalId}/bookmarks/{bookmarkId}` | 회원 | U-011 | `BOOKMARK` |
| API-VIEWER-005 | 북마크 목록 조회 | GET | `/api/v1/rentals/{rentalId}/bookmarks` | 회원 | U-011 | `BOOKMARK` |
| API-REVIEW-001 | 도서 리뷰 목록 조회 | GET | `/api/v1/books/{bookId}/reviews` | 비회원/회원 | U-008 | `REVIEW, RATING, MEMBER` |
| API-REVIEW-002 | 리뷰/별점 작성 | POST | `/api/v1/reviews` | 회원 | U-013 | `REVIEW, RATING, BOOK_RENTAL, POINT_HISTORY` |
| API-REVIEW-003 | 리뷰 수정 | PATCH | `/api/v1/reviews/{reviewId}` | 회원 | U-013 | `REVIEW` |
| API-REVIEW-004 | 리뷰 삭제 | DELETE | `/api/v1/reviews/{reviewId}` | 회원 | U-008, U-013 | `REVIEW` |
| API-FAV-001 | 즐겨찾기 등록 | POST | `/api/v1/me/favorites` | 회원 | U-001, U-006, U-008 | `FAVORITE_BOOK, BOOK` |
| API-FAV-002 | 즐겨찾기 삭제 | DELETE | `/api/v1/me/favorites/{bookId}` | 회원 | U-001, U-006, U-008, U-012 | `FAVORITE_BOOK` |
| API-FAV-003 | 즐겨찾기 목록 조회 | GET | `/api/v1/me/favorites` | 회원 | U-012, U-014 | `FAVORITE_BOOK, BOOK` |
| API-REPORT-001 | 도서 신고 | POST | `/api/v1/reports/books` | 회원 | U-008 신고 모달 | `BOOK_REPORT` |
| API-REPORT-002 | 리뷰 신고 | POST | `/api/v1/reports/reviews` | 회원 | U-008 신고 모달 | `REVIEW_REPORT` |
| API-REPORT-004 | 내 신고 내역 조회 | GET | `/api/v1/me/reports` | 회원 | U-021 | `BOOK_REPORT, REVIEW_REPORT` |
| API-ATTEND-001 | 출석 체크 | POST | `/api/v1/me/attendance` | 회원 | U-016 | `ATTENDANCE, COUPON, MEMBER_COUPON` |
| API-ATTEND-002 | 출석 현황 조회 | GET | `/api/v1/me/attendance` | 회원 | U-016 | `ATTENDANCE` |
| API-POINT-001 | 내 포인트 이력 조회 | GET | `/api/v1/me/points` | 회원 | U-017 | `POINT_HISTORY, MEMBER` |
| API-COUPON-001 | 내 쿠폰 목록 조회 | GET | `/api/v1/me/coupons` | 회원 | U-009, U-017 | `MEMBER_COUPON, COUPON` |
| API-NOTICE-001 | 공지 목록 조회 | GET | `/api/v1/notices` | 비회원/회원 | U-001, U-019 | `NOTICE` |
| API-NOTI-001 | 알림 목록 조회 | GET | `/api/v1/me/notifications` | 회원 | U-019 | `NOTIFICATION` |
| API-NOTI-002 | 알림 읽음 처리 | PATCH | `/api/v1/me/notifications/{notificationId}/read` | 회원 | U-019 | `NOTIFICATION` |
| API-STAT-001 | 내 독서 통계 조회 | GET | `/api/v1/me/statistics` | 회원 | U-026 | `USER_READING_STATISTICS` |
| API-CHALLENGE-001 | 챌린지 목록 조회 | GET | `/api/v1/challenges` | 회원 | U-027 | `CHALLENGE, CHALLENGE_PROGRESS, CHALLENGE_REWARD` |
| API-CHALLENGE-002 | 챌린지 보상 수령 | POST | `/api/v1/challenges/{challengeId}/reward` | 회원 | U-027 | `CHALLENGE_PROGRESS, CHALLENGE_REWARD, POINT_HISTORY, MEMBER_COUPON` |
| API-REQUEST-001 | 희망도서 신청 | POST | `/api/v1/book-requests` | 회원 | U-022 | `BOOK_REQUEST_CANDIDATE, BOOK_REQUEST` |
| API-REQUEST-002 | 내 희망도서 신청 내역 조회 | GET | `/api/v1/me/book-requests` | 회원 | U-023 | `BOOK_REQUEST_CANDIDATE, BOOK_REQUEST, BOOK` |
| API-ADMIN-DASHBOARD-001 | 관리자 대시보드 통계 조회 | GET | `/api/v1/admin/dashboard` | 관리자 | A-001 | `ADMIN_STATISTICS, MEMBER, BOOK, BOOK_RENTAL, PAYMENT, REVIEW, BOOK_REPORT, REVIEW_REPORT, BOOK_REQUEST` |
| API-ADMIN-BOOK-001 | 관리자 도서 목록 조회 | GET | `/api/v1/admin/books` | 관리자 | A-004 | `BOOK, AUTHOR, PUBLISHER, CATEGORY` |
| API-ADMIN-BOOK-002 | 도서 등록 | POST | `/api/v1/admin/books` | 관리자 | A-005 | `PUBLISHER, AUTHOR, BOOK, BOOK_AUTHOR, BOOK_DETAIL, BOOK_FILE, BOOK_PAGE, CATEGORY, BOOK_CATEGORY, BOOK_KEYWORD, BOOK_KEYWORD_MAP, ADMIN_AUDIT_LOG` |
| API-ADMIN-BOOK-003 | 도서 수정 | PUT | `/api/v1/admin/books/{bookId}` | 관리자 | A-006 | `BOOK, BOOK_DETAIL, BOOK_FILE, BOOK_PAGE, BOOK_AUTHOR, BOOK_CATEGORY, BOOK_KEYWORD_MAP, ADMIN_AUDIT_LOG` |
| API-ADMIN-BOOK-004 | 도서 상태 변경 | PATCH | `/api/v1/admin/books/{bookId}/status` | 관리자 | A-004, A-006 | `BOOK, ADMIN_AUDIT_LOG` |
| API-ADMIN-CATEGORY-001 | 관리자 카테고리 목록 조회 | GET | `/api/v1/admin/categories` | 관리자 | A-007 | `CATEGORY` |
| API-ADMIN-CATEGORY-002 | 카테고리 등록/수정 | PUT | `/api/v1/admin/categories/{categoryId}` | 관리자 | A-007 | `CATEGORY, ADMIN_AUDIT_LOG` |
| API-ADMIN-MEMBER-001 | 회원 목록 조회 | GET | `/api/v1/admin/members` | 관리자 | A-002 | `MEMBER, MEMBER_GRADE, SANCTION_HISTORY` |
| API-ADMIN-MEMBER-002 | 회원 상세 조회 | GET | `/api/v1/admin/members/{memberId}` | 관리자 | A-003 | `MEMBER, MEMBER_GRADE, BOOK_RENTAL, PAYMENT, BOOK_REPORT, REVIEW_REPORT, POINT_HISTORY, SANCTION_HISTORY, USER_READING_STATISTICS` |
| API-ADMIN-MEMBER-003 | 회원 상태 변경 | PATCH | `/api/v1/admin/members/{memberId}/status` | 관리자 | A-002, A-003 | `MEMBER, SANCTION_HISTORY, ADMIN_AUDIT_LOG` |
| API-ADMIN-MEMBER-004 | 회원 포인트 조정 | POST | `/api/v1/admin/members/{memberId}/points` | 관리자 | A-003, A-013 | `POINT_HISTORY, MEMBER, ADMIN_AUDIT_LOG` |
| API-ADMIN-RENTAL-001 | 대여 현황 조회 | GET | `/api/v1/admin/rentals` | 관리자 | A-008 | `BOOK_RENTAL, BOOK, MEMBER, READING_PROGRESS, PAYMENT` |
| API-ADMIN-PAYMENT-001 | 결제 목록 조회 | GET | `/api/v1/admin/payments` | 관리자 | A-009 | `PAYMENT, BOOK_RENTAL, MEMBER, BOOK, COUPON_USAGE, POINT_HISTORY` |
| API-ADMIN-REPORT-001 | 신고 목록 조회 | GET | `/api/v1/admin/reports` | 관리자 | A-010 | `BOOK_REPORT, REVIEW_REPORT, MEMBER, BOOK, REVIEW` |
| API-ADMIN-REPORT-002 | 신고 처리 | PATCH | `/api/v1/admin/reports/{targetType}/{reportId}/status` | 관리자 | A-010 | `BOOK_REPORT, REVIEW_REPORT, REVIEW, SANCTION_HISTORY, NOTIFICATION, ADMIN_AUDIT_LOG` |
| API-ADMIN-REQUEST-001 | 희망도서 후보 목록 조회 | GET | `/api/v1/admin/book-request-candidates` | 관리자 | A-011 | `BOOK_REQUEST_CANDIDATE, BOOK_REQUEST, MEMBER` |
| API-ADMIN-REQUEST-002 | 희망도서 승인/반려 | PATCH | `/api/v1/admin/book-request-candidates/{candidateId}/status` | 관리자 | A-011, A-005 | `BOOK_REQUEST_CANDIDATE, BOOK_REQUEST, BOOK, NOTIFICATION, ADMIN_AUDIT_LOG` |
| API-ADMIN-NOTICE-001 | 공지 목록 조회 | GET | `/api/v1/admin/notices` | 관리자 | A-012 | `NOTICE` |
| API-ADMIN-NOTICE-002 | 공지 등록 | POST | `/api/v1/admin/notices` | 관리자 | A-012 | `NOTICE, NOTIFICATION, ADMIN_AUDIT_LOG` |
| API-ADMIN-NOTICE-003 | 공지 수정/숨김 | PUT | `/api/v1/admin/notices/{noticeId}` | 관리자 | A-012 | `NOTICE, ADMIN_AUDIT_LOG` |
| API-ADMIN-COUPON-001 | 쿠폰 목록 조회 | GET | `/api/v1/admin/coupons` | 관리자 | A-013 | `COUPON, MEMBER_COUPON, COUPON_USAGE` |
| API-ADMIN-COUPON-002 | 쿠폰 등록 | POST | `/api/v1/admin/coupons` | 관리자 | A-013 | `COUPON, ADMIN_AUDIT_LOG` |
| API-ADMIN-COUPON-003 | 회원 쿠폰 발급 | POST | `/api/v1/admin/coupons/{couponId}/issue` | 관리자 | A-013 | `MEMBER_COUPON, NOTIFICATION, ADMIN_AUDIT_LOG` |
| API-ADMIN-CHALLENGE-001 | 챌린지 목록 조회 | GET | `/api/v1/admin/challenges` | 관리자 | A-014 | `CHALLENGE, CHALLENGE_REWARD` |
| API-ADMIN-CHALLENGE-002 | 챌린지 등록/수정 | PUT | `/api/v1/admin/challenges/{challengeId}` | 관리자 | A-014 | `CHALLENGE, CHALLENGE_REWARD, ADMIN_AUDIT_LOG` |
| API-ADMIN-LIBRARY-001 | 도서관 목록 조회 | GET | `/api/v1/admin/libraries` | 관리자 | A-015 | `LIBRARY, LIBRARY_BOOK` |
| API-ADMIN-LIBRARY-002 | 도서관 등록/수정 | PUT | `/api/v1/admin/libraries/{libraryId}` | 관리자 | A-015 | `LIBRARY, ADMIN_AUDIT_LOG` |
| API-ADMIN-LIBRARY-003 | 도서관 보유 도서 변경 | PUT | `/api/v1/admin/libraries/{libraryId}/books` | 관리자 | A-015 | `LIBRARY_BOOK, ADMIN_AUDIT_LOG` |
| API-ADMIN-COLLECTION-001 | 컬렉션 목록 조회 | GET | `/api/v1/admin/collections` | 관리자 | A-018 | `COLLECTION, COLLECTION_BOOK` |
| API-ADMIN-COLLECTION-002 | 컬렉션 등록/수정 | PUT | `/api/v1/admin/collections/{collectionId}` | 관리자 | A-018 | `COLLECTION, ADMIN_AUDIT_LOG` |
| API-ADMIN-COLLECTION-003 | 컬렉션 도서 추가/정렬 | PUT | `/api/v1/admin/collections/{collectionId}/books` | 관리자 | A-018 | `COLLECTION_BOOK, BOOK, ADMIN_AUDIT_LOG` |
| API-ADMIN-COLLECTION-004 | 컬렉션 도서 제외/상태 변경 | PATCH | `/api/v1/admin/collections/{collectionId}` | 관리자 | A-018 | `COLLECTION, COLLECTION_BOOK, ADMIN_AUDIT_LOG` |
| API-ADMIN-STAT-001 | 관리자 통계 조회 | GET | `/api/v1/admin/statistics` | 관리자 | A-016 | `ADMIN_STATISTICS` |
| API-ADMIN-AUDIT-001 | 관리자 감사 로그 조회 | GET | `/api/v1/admin/audit-logs` | 관리자 | A-017 | `ADMIN_AUDIT_LOG, MEMBER` |


---

## 4. 상세 API 명세

### 4.1 Auth / My Page

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-AUTH-001 | Body: `loginId`, `password`, `passwordConfirm`, `name`, `email`, `phone?`, `birthDate`, `gender?` | `memberId`, `loginId`, `name`, `email`, `role=USER`, `status=JOINED` | `birthDate` 필수, 미래 날짜 불가. 아이디/이메일 중복 검증. 약관 동의는 저장하지 않음. |
| API-AUTH-002 | Body: `loginId`, `password`, `rememberMe?` | `accessToken`, `tokenType`, `expiresIn`, `member`, `redirectScreen` | `JOINED`만 로그인 성공. `ADMIN`은 `A-001`, `USER`는 `U-001`. 생일 쿠폰은 로그인 성공 시 당일 1회 발급 가능. |
| API-AUTH-003 | Body: `name`, `email` | `found`, `loginId(masked)`, `joinedAt` | 이름+이메일 일치 시 아이디 반환. 비밀번호 재설정 API는 final 범위에서 제외. |
| API-ME-001 | Header: Bearer token | 회원 기본정보, 등급, 포인트, 쿠폰 수, 대여 수, 완독 수, 즐겨찾기 수, 미읽은 알림 수 | `loginId`, `birthDate`는 읽기 전용. 등급 달성률은 `PAYMENT.amount` 집계로 계산. |
| API-ME-002 | Body: `name?`, `email?`, `phone?`, `gender?` | 수정된 회원정보 | `loginId`, `birthDate`, `role`, `status` 수정 불가. 이메일 중복 검증. |
| API-ME-003 | Body: `currentPassword`, `newPassword`, `newPasswordConfirm` | `changedAt` | 로그인 세션과 현재 비밀번호 검증 후 변경. 별도 토큰 테이블 없음. |
| API-ME-004 | Body: `currentPassword`, `withdrawalReason?` | `status=WITHDRAWN`, `withdrawnAt` | 물리 삭제하지 않음. 진행 중 대여가 있으면 탈퇴 불가. |

### 4.2 도서 / 카테고리 / 컬렉션 / 도서관

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-BOOK-001 | Query: `categoryId?`, `parentCategoryId?`, `rentalType?`, `section?`, `sort?`, `page?`, `size?` | `page<BookCard>` | User에는 `BOOK.status=AVAILABLE`만 노출. 카드에는 표지/제목/저자/출판사/무료유료/가격/즐겨찾기 여부 반환. |
| API-BOOK-002 | Query: `q`, `categoryId?`, `rentalType?`, `sort?`, `page?`, `size?` | `keyword`, `totalCount`, `page<BookCard>` | 검색 대상은 제목, 저자, 출판사, ISBN, 키워드. 결과 없음이면 희망도서 신청 안내. |
| API-BOOK-003 | Path: `bookId` | 기본 정보, 저자, 출판사, 카테고리, 키워드, 대여 정보, 전체 페이지, 리뷰 요약, `myRentalState?`, `myFavoriteYn?` | 미리보기는 제외. TTS 가능 여부/청취 위치는 저장하지 않음. |
| API-BOOK-005 | Query: `baseBookId?`, `type?`, `limit?` | 추천 기준, `books<BookCard>` | 같은 작가/카테고리/키워드 또는 메인 추천. 추천 로그 저장 안 함. |
| API-CATEGORY-001 | Query: `includeChildren=true` | 카테고리 트리 | `ACTIVE` 카테고리만 User 메뉴 노출, `displayOrder` 기준 정렬. |
| API-COLLECTION-001 | Query: `collectionType?`, `status=ACTIVE`, `page?`, `size?`, `previewSize?` | 컬렉션 목록 | 시리즈/이벤트 컬렉션 공개 조회. User는 `ACTIVE`만 노출. 메인 화면에서는 활성 컬렉션과 컬렉션별 대표 도서 5~6개를 미리보기로 반환한다. `U-019`가 직접 호출하는 API가 아니라 EVENT 알림 클릭 후 `U-006` 이벤트 필터 또는 컬렉션 목록 진입 시 호출한다. |
| API-COLLECTION-002 | Path: `collectionId` | 컬렉션 상세, 도서 카드 목록 | `SERIES`는 `displayOrder`, `EVENT`는 운영 정렬. 등록된 `BOOK`만 포함. EVENT 알림의 target 이동 후 상세 목록을 구성할 때 사용할 수 있다. |
| API-LIBRARY-001 | Path: `bookId` | 도서 요약, 도서관명, 주소, 위도, 경도, 보유 상태 | 실물 대여/예약 없음. 지도 표시는 프론트 외부 지도 API가 처리. |

### 4.3 대여 / 결제 / 뷰어

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-RENTAL-001 | Body: `bookId` | `rentalId`, `bookId`, `rentalStatus=READY`, `rentalEndAt` | 무료 도서만 허용. 같은 책 READY/READING 대여 중이면 409. `rentalStartAt`은 첫 뷰어 진입 전까지 `null`. |
| API-RENTAL-003 | Query: `status?`, `page?`, `size?` | 내 대여 목록, 진행률 | 내 서재는 `READY`, `READING`, `OWNED`만 표시. 반납/연체 상태 없음. |
| API-READING-001 | Query: `limit?` | 최근 읽은 책, `rentalId`, 현재/전체 페이지, 진행률, 마지막 읽은 시각 | `READING_PROGRESS.lastReadAt` 내림차순. |
| API-PAYMENT-001 | Body: `bookId`, `memberCouponId?`, `pointAmount?`, `cardApprovalToken(dummy)` | `rentalId`, `paymentId`, `originalAmount`, `couponDiscountAmount`, `pointUsedAmount`, `amount`, `CARD`, `PAID` | 유료 도서만 허용. 쿠폰/포인트는 할인/차감 수단. 최종 0원이어도 `PAYMENT` 저장. 실패/취소/환불 이력 없음. |
| API-PAYMENT-003 | Query: `page?`, `size?`, `from?`, `to?` | 완료 결제 목록과 할인 상세 | `paymentType=RENTAL`, `paymentMethod=CARD`, `paymentStatus=PAID`만 반환. |
| API-VIEWER-001 | Path: `rentalId`, `pageNo` | 제목, 페이지 본문, 전체 페이지, 진행률, 북마크 여부 | 첫 뷰어 진입이면 `READY`→`READING`, `rentalStartAt` 저장. 자동 넘김/TTS 설정은 DB 저장 안 함. |
| API-VIEWER-002 | Body: `currentPage`, `action?` | 진행 ID, 현재 페이지, 진행률, 완독 여부 | 처음으로 가기는 1페이지 저장. 마지막 페이지 도달 시 최초 1회 완독 처리. |
| API-VIEWER-003 | Body: `pageNo` | `bookmarkId`, `pageNo`, `createdAt` | 같은 대여의 같은 페이지 북마크 1개만 허용. 메모 저장 안 함. |
| API-VIEWER-004 | Path: `rentalId`, `bookmarkId` | `deleted=true` | 본인 대여 건 북마크만 삭제. |
| API-VIEWER-005 | Path: `rentalId` | 북마크 목록 | `pageNo` 오름차순 반환. |


### 4.4 리뷰 / 즐겨찾기 / 신고

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-REVIEW-001 | Path: `bookId`, Query: `page?`, `size?`, `sort?` | 별점 요약, 활성 리뷰 목록 | User 목록에는 `REVIEW.status=ACTIVE`만 노출. 평균은 `RATING`, 리뷰 수는 활성 리뷰 기준. |
| API-REVIEW-002 | Body: `bookId`, `ratingScore(1~5)`, `content` | `reviewId`, `ratingId?`, `status=ACTIVE` | 첫 읽기 시작 이후 작성 가능. `REVIEW`/`RATING` 모두 회원+도서 기준 1개. 별점은 수정/삭제 불가. `DELETED` 리뷰는 같은 row 복구 가능. DB 테이블은 `REVIEW`와 `RATING`으로 분리하지만 API는 리뷰 작성 흐름에서 함께 생성하는 통합 엔드포인트다. |
| API-REVIEW-003 | Path: `reviewId`, Body: `content` | 수정된 리뷰 | 본인 리뷰만 수정. 별점 수정 불가. `DELETED` 리뷰 수정 시 `ACTIVE` 복구 가능. |
| API-REVIEW-004 | Path: `reviewId` | `status=DELETED` | 물리 삭제하지 않음. 관리자 숨김 `HIDDEN`과 사용자 삭제 `DELETED` 구분. |
| API-FAV-001 | Body: `bookId` | `favoriteId`, `bookId` | 회원+도서 기준 1개. 비회원은 로그인 유도. |
| API-FAV-002 | Path: `bookId` | `deleted=true` | 삭제 후 현재 화면 즐겨찾기 상태 갱신. |
| API-FAV-003 | Query: `sort?`, `page?`, `size?` | 즐겨찾기 도서 목록 | 빈 목록이면 도서 둘러보기 안내. |
| API-REPORT-001 | Body: `bookId`, `reportType`, `content` | `reportId`, `targetType=BOOK`, `status=WAITING` | 같은 사용자의 동일 책 재신고 허용. 별도 신고 화면 없이 모달 접수. |
| API-REPORT-002 | Body: `reviewId`, `reportType`, `content` | `reportId`, `targetType=REVIEW`, `status=WAITING` | 같은 사용자의 동일 리뷰 재신고 허용. 리뷰 신고는 관리자 제재 흐름과 연결 가능. |
| API-REPORT-004 | Query: `targetType?`, `status?`, `page?`, `size?` | 내 책/리뷰 신고 내역 | 회원 대상 신고 내역 없음. 상태 라벨은 접수/대기, 처리 중, 완료, 반려. |

### 4.5 출석 / 포인트 / 쿠폰 / 알림 / 통계 / 챌린지 / 희망도서

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-ATTEND-001 | Body 없음 | 출석일, 연속 출석일, 발급 쿠폰 목록 | 회원+날짜 1회 저장. 출석 자체 포인트 지급 없음. 7일 5%, 30일 10% 쿠폰 지급 가능. |
| API-ATTEND-002 | Query: `yearMonth(YYYY-MM)` | 월별 출석일, 오늘 출석 여부, 연속 출석일 | 달력 표시용 데이터 반환. |
| API-POINT-001 | Query: `pointType?`, `page?`, `size?` | 포인트 잔액, 이력 목록 | 허용 유형은 `CHALLENGE_REWARD`, `REVIEW_REWARD`, `PAYBACK`, `EVENT_REWARD`, `PAYMENT_USE`, `ADMIN_ADJUST`. |
| API-COUPON-001 | Query: `status?`, `usableForBookId?`, `page?`, `size?` | 회원 쿠폰 목록 | 기본 유효기간 14일. 같은 결제에 쿠폰 1장만 사용. |
| API-NOTICE-001 | Query: `page?`, `size?` | 활성 공지 목록과 내용 | User에는 `noticeStatus=ACTIVE`만 노출. 상세는 모달 표시. |
| API-NOTI-001 | Query: `readYn?`, `notificationType?`, `page?`, `size?` | 미읽은 수, 알림 목록 | 알림 본문은 저장/반환하지 않음. 생성 후 1개월 지난 알림은 삭제 대상. |
| API-NOTI-002 | Path: `notificationId` | 읽음 처리 결과, 연결 대상 | 알림 행 클릭 시 읽음 처리 후 target 화면으로 이동. |
| API-STAT-001 | Query: `periodType`, `baseDate?` | 대여 수, 완독 수, 읽은 페이지, 리뷰 수, 즐겨찾기 수, 탄소/나무 계산값, 시계열 | 탄소 절약량=완독 수×2kg, 나무 보호량=완독 수×0.04그루. 계산값은 DB 저장 안 함. |
| API-CHALLENGE-001 | Query: `challengeType?` | 챌린지 목록, 진행률, 보상 | `DAILY`, `TOTAL`만 운영. 전체 챌린지는 가입 이후 1회 달성. |
| API-CHALLENGE-002 | Path: `challengeId` | 보상 수령 시각, 지급 포인트/쿠폰 | 달성했고 미수령인 챌린지만 지급. 포인트는 `CHALLENGE_REWARD`. |
| API-REQUEST-001 | Body: `title`, `author`, `publisher`, `reason` | `requestId`, `requestCandidateId`, `requestStatus=REQUESTED`, `requestCount` | 제목+저자+출판사 정규화 후 후보 단위 집계. 같은 회원 중복 신청은 저장하지 않음. ISBN 저장 없음. |
| API-REQUEST-002 | Query: `status?`, `page?`, `size?` | 내 신청 목록, 후보 신청자 수, 승인 도서, 반려 사유 | 회원별 신청 상태와 후보 신청자 수를 함께 반환. |


### 4.6 관리자 - 대시보드 / 도서 / 카테고리

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-ADMIN-DASHBOARD-001 | 없음 | KPI 카드, 최근 신고, 최근 희망도서, 최근 결제 | 전체 회원, 제재 회원, 이용 가능 도서, 전체 대여, 완독, 결제 합계, 리뷰, 신고 KPI 반환. |
| API-ADMIN-BOOK-001 | Query: `q?`, `categoryId?`, `rentalType?`, `status?`, `page?`, `size?` | 관리자 도서 목록 | `HIDDEN`, `INACTIVE` 포함 조회 가능. 제목/저자/출판사/ISBN/카테고리/유형/상태 검색. |
| API-ADMIN-BOOK-002 | Body: `title`, `isbn?`, `publisherName`, `authors[]`, `categoryIds[]`, `keywords[]`, `rentalType`, `rentalPrice`, `defaultRentalDays`, `coverImageUrl?`, `status`, `detail`, `file`, `pages[]` | `bookId`, `status`, `createdAt` | ISBN 중복 검증. 유료 도서 가격 0원 불가. 페이지 번호 중복 불가. 상세 입력은 소개/목차/출판사 리뷰/상세 내용으로 제한. 감사 로그 기록. |
| API-ADMIN-BOOK-003 | Path: `bookId`, Body: 도서 수정 필드 | `bookId`, `updatedAt` | 출판사명 포함 수정. 수정 전후 핵심 값 감사 로그 기록. |
| API-ADMIN-BOOK-004 | Path: `bookId`, Body: `status(AVAILABLE/HIDDEN/INACTIVE)`, `reason?` | `bookId`, `status` | 물리 삭제하지 않고 상태 변경. `HIDDEN`/`INACTIVE`는 User 목록 제외. |
| API-ADMIN-CATEGORY-001 | Query: `includeInactive=true` | 카테고리 트리 | 활성/비활성 전체 조회. |
| API-ADMIN-CATEGORY-002 | Path: `categoryId`, Body: `parentCategoryId?`, `categoryName`, `displayOrder`, `status` | 카테고리 정보 | 상위/하위 카테고리와 표시 순서 관리. 비활성은 User 메뉴 제외. |

### 4.7 관리자 - 회원 / 대여 / 결제 / 신고 / 희망도서

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-ADMIN-MEMBER-001 | Query: `q?`, `status?`, `role?`, `gradeId?`, `page?`, `size?` | 회원 목록, 현재 유효 제재 요약 | 검색 대상은 이름/아이디/이메일. |
| API-ADMIN-MEMBER-002 | Path: `memberId` | 회원 상세, 이용 요약, 대여/결제/신고/포인트/제재 탭 | 관리자는 비밀번호를 확인하거나 변경하지 않음. |
| API-ADMIN-MEMBER-003 | Path: `memberId`, Body: `status`, `reason`, `sanctionType?`, `startedAt?`, `endedAt?` | 변경된 회원 상태, 현재 제재 | 필요 시 `SANCTION_HISTORY` 저장. `ACCOUNT_SUSPENSION`은 기간 동안 로그인 불가. 감사 로그 기록. |
| API-ADMIN-MEMBER-004 | Path: `memberId`, Body: `pointAmount(+/-)`, `description` | 포인트 이력, 잔액 | `pointType=ADMIN_ADJUST`. 잔액 음수 불가. |
| API-ADMIN-RENTAL-001 | Query: `q?`, `memberId?`, `status?`, `page?`, `size?` | 대여 현황과 진행률 | 대여 관리는 조회 전용. `READY`, `READING`, `OWNED`만 제공. |
| API-ADMIN-PAYMENT-001 | Query: `q?`, `from?`, `to?`, `page?`, `size?` | 결제 목록, 쿠폰/포인트 차감 상세 | `RENTAL`/`CARD`/`PAID`만 조회. 취소/환불/복구 없음. |
| API-ADMIN-REPORT-001 | Query: `targetType?`, `status?`, `page?`, `size?` | 책/리뷰 신고 목록 | 회원 대상 신고 없음. 상태는 `WAITING`, `PROCESSING`, `DONE`, `REJECTED`. |
| API-ADMIN-REPORT-002 | Path: `targetType`, `reportId`, Body: `status`, `processResult?`, `hideReviewYn?`, `sanction?` | 처리된 신고 | 리뷰 신고 처리 중 `REVIEW.status=HIDDEN` 가능. 제재 시 `SANCTION_HISTORY` 저장 및 필요 시 회원 상태 갱신. REPORT 알림 생성. |
| API-ADMIN-REQUEST-001 | Query: `status?`, `q?`, `page?`, `size?` | 희망도서 후보 목록, 신청자 요약 | 후보 제목/저자/출판사, 신청자 수, 최초 신청일 반환. final ERD 저장 상태는 `REQUESTED`, `APPROVED`, `REJECTED`. |
| API-ADMIN-REQUEST-002 | Path: `candidateId`, Body: `status(APPROVED/REJECTED)`, `approvedBookId?`, `rejectReason?` | 후보 상태, 영향 신청 수 | 도서 등록 후 `approvedBookId` 연결. 연결된 회원별 신청 row 일괄 승인/반려. BOOK_REQUEST 알림 생성. |


### 4.8 관리자 - 공지 / 쿠폰 / 챌린지 / 도서관 / 컬렉션 / 통계 / 감사

| API ID | Request | Response data | 주요 정책/검증 |
|---|---|---|---|
| API-ADMIN-NOTICE-001 | Query: `status?`, `page?`, `size?` | 관리자 공지 목록 | 숨김 공지도 조회 가능. |
| API-ADMIN-NOTICE-002 | Body: `title`, `content`, `noticeStatus`, `notifyYn` | `noticeId`, `notifyCreatedCount` | `notifyYn=true`이면 회원별 NOTICE 알림 생성. 알림 본문 없음. |
| API-ADMIN-NOTICE-003 | Path: `noticeId`, Body: `title?`, `content?`, `noticeStatus?`, `notifyYn?` | 수정된 공지 | 공지 수정 시 기존 회원 알림 재생성 안 함. `HIDDEN`은 User 제외. |
| API-ADMIN-COUPON-001 | Query: `status?`, `couponType?`, `page?`, `size?` | 쿠폰 정의와 발급/사용/만료 현황 | 쿠폰 운영 조회. |
| API-ADMIN-COUPON-002 | Body: `couponName`, `couponType`, `benefitValue`, `benefitUnit`, `validDays=14`, `status` | `couponId` | 5/10/100% 쿠폰은 `PERCENT_DISCOUNT` + `PERCENT`. 기본 유효기간 14일. |
| API-ADMIN-COUPON-003 | Path: `couponId`, Body: `memberIds[]`, `quantity` | `issuedCount` | 대상 회원에게 `MEMBER_COUPON` 생성. COUPON 알림 생성 가능. |
| API-ADMIN-CHALLENGE-001 | Query: `challengeType?`, `status?`, `page?`, `size?` | 챌린지와 보상 목록 | `DAILY`, `TOTAL`만 관리. |
| API-ADMIN-CHALLENGE-002 | Path: `challengeId`, Body: `challengeName`, `challengeType`, `targetAction`, `targetCount`, `status`, `rewards[]` | `challengeId` | 보상은 `POINT` 또는 `COUPON`과 지급 수량. 비활성은 신규 노출 제외. |
| API-ADMIN-LIBRARY-001 | Query: `q?`, `status?`, `page?`, `size?` | 도서관 목록, 좌표, 보유 도서 수 | 좌표 보유 여부 포함. |
| API-ADMIN-LIBRARY-002 | Path: `libraryId`, Body: `libraryName`, `address`, `latitude?`, `longitude?`, `status` | `libraryId` | 도서관 기본 정보 등록/수정용. 좌표 null 허용. 실물 대여/예약 관리 없음. A-015의 `수정` 버튼에서 보유 도서 변경도 함께 있으면 002 성공 후 003을 순차 호출한다. |
| API-ADMIN-LIBRARY-003 | Path: `libraryId`, Body: `books[{bookId, holdingStatus}]` | `libraryId`, `mappedCount` | 도서관 보유 도서 매핑 변경용. 도서관+도서 기준 1개. A-015의 `수정` 버튼에서 보유 도서 변경분이 있을 때 002와 분리해 호출한다. User 위치 화면에는 활성 도서관과 보유 상태 반환. |
| API-ADMIN-COLLECTION-001 | Query: `collectionType?`, `status?`, `page?`, `size?` | 컬렉션 목록, 등록 도서 수 | 시리즈/이벤트 관리 목록. |
| API-ADMIN-COLLECTION-002 | Path: `collectionId`, Body: `name`, `collectionType`, `discountRate?`, `description?`, `startDate?`, `endDate?`, `status` | `collectionId` | `SERIES`는 순서 있는 책 묶음, `EVENT`는 이벤트 묶음. |
| API-ADMIN-COLLECTION-003 | Path: `collectionId`, Body: `books[{bookId, displayOrder?}]` | `collectionId`, `mappedCount` | 서비스에 등록된 BOOK만 추가. `SERIES`는 `displayOrder` 사용. |
| API-ADMIN-COLLECTION-004 | Path: `collectionId`, Body: `status?`, `removeBookIds[]?` | 컬렉션 상태, 매핑 수 | 도서 제외는 컬렉션 묶음에서만 제거. 숨김/종료는 User 노출 제외. |
| API-ADMIN-STAT-001 | Query: `periodType`, `baseDate?`, `ageBand?` | KPI, 추이, 나이대별 통계, 탄소/나무 계산값 | 나이대는 `MEMBER.birthDate` 기준 `10S`, `20S`, `30S`, `40S`, `50_PLUS`. |
| API-ADMIN-AUDIT-001 | Query: `actionType?`, `targetType?`, `targetId?`, `page?`, `size?` | 감사 로그 목록 | 조회 전용. 수정/삭제 API 없음. |

---

## 5. 제외/삭제 API

final 화면설계와 final ERD 기준으로 아래 API는 기존 `api-spec.md` 예시에서 제거한다.

| 제거 API | 제거 사유 |
|---|---|
| `API-AUTH-004`, `API-AUTH-005` 비밀번호 재설정 | 비회원 화면은 회원가입/로그인/아이디 찾기만 제공. 비밀번호 변경은 로그인 후 회원정보 수정에서 처리 |
| `API-BOOK-004` 도서 미리보기 페이지 조회 | final 화면에 미리보기 기능 없음. 본문은 대여 후 뷰어 API에서 조회 |
| `API-RENTAL-002` 반납 | 반납 기능 제외. 대여 상태는 `READY`, `READING`, `OWNED`만 사용 |
| `API-PAYMENT-002` 연체료 결제 | 연체/연체료 정책 제외 |
| `API-REPORT-003` 사용자 신고 | 회원 대상 신고 테이블 없음. 책/리뷰 신고만 제공 |
| `API-ADMIN-RENTAL-002` 대여 상태 변경 | 관리자 대여 관리는 조회 전용 |
| `API-ADMIN-PAYMENT-002` 결제 취소/환불/복구 | 완료 결제만 저장하며 취소/환불/복구 이력은 저장하지 않음 |

---

## 6. 구현 메모

1. 모든 금액 필드는 `DECIMAL` 성격으로 응답하되 JSON에서는 number로 반환한다.
2. 날짜/시각은 ISO-8601 문자열(`YYYY-MM-DD`, `YYYY-MM-DDTHH:mm:ss`)로 반환한다.
3. User 공개 API는 `BOOK.status=AVAILABLE`, `CATEGORY.status=ACTIVE`, `NOTICE.noticeStatus=ACTIVE`만 노출한다.
4. 관리자 변경성 API는 가능한 경우 `ADMIN_AUDIT_LOG`에 변경 전/후 요약을 저장한다.
5. 결제와 유료 대여 생성, 쿠폰 사용, 포인트 차감, 회원 포인트 잔액 변경은 하나의 트랜잭션으로 처리한다.
6. 마지막 페이지 도달에 따른 완독 처리와 챌린지/통계 갱신은 중복 처리되지 않도록 멱등하게 구현한다.
7. 알림은 본문 없이 제목과 연결 대상만 저장하고, 생성 후 한 달이 지난 알림은 정리 대상이다.
8. 한 화면에서 여러 API를 호출할 수 있다. 예를 들어 `U-008`은 도서 상세/추천/리뷰/즐겨찾기/신고/도서관 API를 조합하고, `A-015`의 수정 버튼은 기본 정보 변경과 보유 도서 변경 여부에 따라 `API-ADMIN-LIBRARY-002`와 `API-ADMIN-LIBRARY-003`을 순차 호출할 수 있다.
9. DB 테이블 분리와 API 분리는 동일하지 않다. 예를 들어 `REVIEW`와 `RATING`은 ERD에서 분리하지만 `API-REVIEW-002`는 리뷰 작성 화면 흐름에 맞춰 리뷰와 별점을 함께 생성한다.
