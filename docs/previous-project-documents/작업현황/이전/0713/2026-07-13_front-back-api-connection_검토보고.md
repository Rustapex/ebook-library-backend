# Front-Back API 연결 여부 검토보고

작성일: 2026-07-13

## 1. 검토 범위

- Front 기준 문서: `C:\Github\abc-proj-win-FRONT\docs\ABC_front_화면.md`
  - User API 표: 240-288행
  - 보강 필요사항: 375-388행
  - 구현 제외 기능: 356-364행
- Admin Front 기준 문서: `C:\Github\abc-proj-win-FRONT\docs\ABC_front_Admin_화면.md`
  - Admin API Connection Table: 103-140행
- Front 실제 호출부: `C:\Github\abc-proj-win-FRONT\abc_front\src\api\*.ts`
  - `apiClient.ts`는 `VITE_API_BASE_URL`을 baseURL로 사용하며, 호출부는 `/auth/signup` 같은 `/api/v1` 하위 경로를 사용한다.
- Back 실제 구현 기준: `C:\Github\abc-proj-win\src\main\java\com\acorn\abc\**\*Controller.java`
  - `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`만 대조했다.

## 2. 판정 기준

- 연결됨: Front 문서 또는 실제 `src/api` 호출 경로와 Back Controller 매핑이 일치한다.
- 미연결: Front 문서 또는 실제 `src/api` 호출 경로는 있으나 Back Controller 매핑이 없다.
- Front 미연결/미확인: Back Controller 매핑은 있으나 실제 Front API wrapper 호출이 확인되지 않았다.
- 이번 검토는 소스 수정 없이 경로 연결 여부만 확인했다. DTO 필드, 인증 권한, 런타임 정상 동작은 검증하지 않았다.

## 3. 도메인별 결과

### 인증/회원 API (가입/로그인/찾기/재설정)

| 상태 | API |
|---|---|
| 연결됨 | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `PATCH /api/v1/me/password` |
| Front 미연결/미확인 | `POST /api/v1/auth/find-id`는 Back `AuthController`에 있으나, 실제 Front `src` 검색에서는 `/auth/find-id` 호출부가 확인되지 않았다. 문서에는 U-004 API로 존재한다. |
| 미연결 | `GET /api/v1/me`, `PATCH /api/v1/me`, `DELETE /api/v1/me` |
| 범위 제외 | U-005 비회원 비밀번호 재설정은 Front 문서상 final API 범위 제외 |

문제 도메인: 회원 프로필 조회/수정/탈퇴. 로그인 회원 비밀번호 변경만 연결되어 있다.

### 도서/카테고리/컬렉션 API

| 상태 | API |
|---|---|
| 연결됨 | `GET /api/v1/books`, `GET /api/v1/books/search`, `GET /api/v1/books/{bookId}`, `GET /api/v1/books/recommendations` |
| 연결됨 | `GET /api/v1/categories` |
| 연결됨 | `GET /api/v1/collections`, `GET /api/v1/collections/{collectionId}` |
| 연결됨 | `GET /api/v1/books/{bookId}/libraries` |

User 도서/카테고리/컬렉션 조회 축은 연결되어 있다. 단, Admin 도서/카테고리/컬렉션 관리는 관리자 API 도메인에서 별도로 대부분 미연결이다.

### 대여/결제 API

| 상태 | API |
|---|---|
| 연결됨 | `POST /api/v1/rentals/free`, `POST /api/v1/rentals/paid` |
| 연결됨 | `GET /api/v1/me/rentals` |
| 연결됨 | `GET /api/v1/me/payments` |
| 미연결 | `GET /api/v1/admin/rentals`, `GET /api/v1/admin/payments` |

User 대여/결제는 연결되어 있고, 관리자 대여/결제 조회는 Back Controller가 없다.

### 전자책 뷰어/북마크/진행 API

| 상태 | API |
|---|---|
| 연결됨 | `GET /api/v1/rentals/{rentalId}/pages/{pageNo}` |
| 연결됨 | `PUT /api/v1/rentals/{rentalId}/progress` |
| 연결됨 | `GET /api/v1/rentals/{rentalId}/bookmarks` |
| 연결됨 | `POST /api/v1/rentals/{rentalId}/bookmarks` |
| 연결됨 | `DELETE /api/v1/rentals/{rentalId}/bookmarks/{bookmarkId}` |

이 도메인은 경로 기준으로 연결되어 있다.

### 리뷰/별점/즐겨찾기 API

| 상태 | API |
|---|---|
| 연결됨 | `GET /api/v1/books/{bookId}/reviews` |
| 연결됨 | `POST /api/v1/reviews`, `PATCH /api/v1/reviews/{reviewId}`, `DELETE /api/v1/reviews/{reviewId}` |
| 연결됨 | `GET /api/v1/me/favorites`, `POST /api/v1/me/favorites`, `DELETE /api/v1/me/favorites/{bookId}` |
| 미연결 | `POST /api/v1/reports/books`, `POST /api/v1/reports/reviews`, `GET /api/v1/me/reports` |

리뷰/별점/즐겨찾기는 연결되어 있다. 신고 API는 Front 호출부와 문서에는 있으나 Back `ReportController`가 없어 미연결이다.

### 챌린지/출석/포인트/쿠폰 API

| 상태 | API |
|---|---|
| 연결됨 | `GET /api/v1/challenges`, `POST /api/v1/challenges/{challengeId}/reward` |
| 연결됨 | `GET /api/v1/me/attendance`, `POST /api/v1/me/attendance` |
| 연결됨 | `GET /api/v1/me/points`, `GET /api/v1/me/coupons` |
| 연결됨 | `GET /api/v1/admin/challenges`, `PUT /api/v1/admin/challenges/{challengeId}` |
| 연결됨 | `GET /api/v1/admin/coupons`, `POST /api/v1/admin/coupons`, `POST /api/v1/admin/coupons/{couponId}/issue` |
| 부분 연결 | `POST /api/v1/admin/members/{memberId}/points`만 연결됨. 관리자 회원 목록/상세/상태 API는 없다. |

User 기능은 연결되어 있다. Admin 쿠폰/포인트 화면은 포인트 조정 자체는 가능하지만 회원 검색/상세/상태 변경 API가 빠져 있어 화면 단위로는 부분 연결이다.

### 희망도서/알림/공지 API

| 상태 | API |
|---|---|
| 미연결 | `POST /api/v1/book-requests`, `GET /api/v1/me/book-requests` |
| 미연결 | `GET /api/v1/me/notifications`, `PATCH /api/v1/me/notifications/{notificationId}/read` |
| 미연결 | `GET /api/v1/notices`, `GET /api/v1/notices/{noticeId}` |
| 미연결 | `GET /api/v1/admin/book-request-candidates`, `PATCH /api/v1/admin/book-request-candidates/{candidateId}/status` |
| 미연결 | `GET /api/v1/admin/notices`, `POST /api/v1/admin/notices`, `PUT /api/v1/admin/notices/{noticeId}` |

가장 큰 미연결 도메인이다. User/관리자 모두 Back Controller가 없다.

### 관리자 API (대시보드/통계/감사로그 등)

| 상태 | API |
|---|---|
| 연결됨 | `GET /api/v1/admin/dashboard` |
| 연결됨 | `GET /api/v1/admin/statistics` |
| 연결됨 | `GET /api/v1/admin/audit-logs` |
| 연결됨 | `GET/POST /api/v1/admin/coupons`, `POST /api/v1/admin/coupons/{couponId}/issue` |
| 연결됨 | `GET /api/v1/admin/challenges`, `PUT /api/v1/admin/challenges/{challengeId}` |
| 부분 연결 | `POST /api/v1/admin/members/{memberId}/points`만 있음 |
| 미연결 | `GET /api/v1/admin/members`, `GET /api/v1/admin/members/{memberId}`, `PATCH /api/v1/admin/members/{memberId}/status` |
| 미연결 | `GET/POST/PUT/PATCH /api/v1/admin/books...` |
| 미연결 | `GET/PUT /api/v1/admin/categories...` |
| 미연결 | `GET /api/v1/admin/rentals`, `GET /api/v1/admin/payments` |
| 미연결 | `GET/PATCH /api/v1/admin/reports...` |
| 미연결 | `GET/PATCH /api/v1/admin/book-request-candidates...` |
| 미연결 | `GET/POST/PUT /api/v1/admin/notices...` |
| 미연결 | `GET/PUT /api/v1/admin/libraries...` |
| 미연결 | `GET/PUT/PATCH /api/v1/admin/collections...` |

관리자 API는 일부 통계/쿠폰/챌린지/감사로그만 연결되어 있고, 운영 CRUD 성격의 관리자 도메인은 대부분 미구현 상태다.

## 4. 우선 문제 도메인 요약

1. 희망도서/알림/공지: User와 Admin 모두 미연결.
2. 관리자 운영 CRUD: 회원 목록/상세/상태, 도서, 카테고리, 대여, 결제, 신고, 희망도서, 공지, 도서관, 컬렉션이 미연결.
3. 회원 프로필: `GET/PATCH/DELETE /api/v1/me` 미연결. 비밀번호 변경만 연결.
4. 신고: User 신고 작성/내역과 Admin 신고 처리 모두 미연결.
5. 인증 찾기: Back에는 `POST /api/v1/auth/find-id`가 있으나 실제 Front API 호출부가 확인되지 않았다.

## 5. 백엔드에 있으나 Front 문서 주요 표와 직접 연결되지 않은 API

- `GET /api/v1/admin/books/external/lookup`
- `/api/v1/admin/book-isbn-temps` 하위 seed/enrich/fetch/list/approve/bulk-approve API

위 API는 백엔드에는 있으나 이번 Front 문서의 Admin API Connection Table에는 직접 매핑되어 있지 않았다.

## 6. 검증 제한

- 실제 서버 기동, API 호출, 빌드, 테스트는 수행하지 않았다.
- Controller route 존재 여부만 본 것이므로 request/response DTO 필드 불일치, 인증/권한 문제, 서비스 내부 예외는 별도 검증이 필요하다.
- Front 전체 페이지 컴포넌트의 직접 `apiClient` 사용 여부는 필요한 키워드 중심으로만 검색했다. 주요 API wrapper는 `src/api/*.ts` 기준으로 확인했다.
