# API ID별 백엔드 구현·프론트 연결·화면 구현 현황

작성일: 2026-07-21
기준 상태: PR #53·#55 병합 diff와 현재 로컬 소스의 targeted 재조사 결과
기준 명세: `docs/API 명세/api-spec(final).md`의 `3. API 목록 요약`

## 1. 판정 범위와 기준

- 대상: 최종 API 명세 요약표의 API ID 81개 + 설계 이후 추가된 `API-AUTH-006` 1개 + 현재 구현 기준 관리자 컬렉션 `API-ADMIN-COLLECTION-005`·`006` 2개 + PR #52에서 추가된 관리자 도서 `API-ADMIN-BOOK-005` 1개 = 85개
- 백엔드 구현 `O`: API 명세의 HTTP Method와 URL에 대응하는 Spring Controller 매핑이 존재함
- 백엔드 구현 `X`: 대응하는 Controller 매핑을 찾지 못함
- 프론트 연결 `O`: 백엔드 구현이 `O`이고, Front API 호출부와 화면 또는 Context 사용이 확인됨
- 프론트 연결 `X`: 백엔드 또는 Front 호출부 중 하나 이상이 없거나, wrapper만 있고 화면에서 사용하지 않음
- 화면 구현 `O`: API 명세의 화면 ID에 대응하는 컴포넌트와 route가 존재함
- `U-013`은 독립 route가 아니라 설계대로 `U-008` 도서 상세 내부의 리뷰 작성·수정·삭제 영역으로 구현되어 `O`로 판정함
- 실제 서버 실행, HTTP 응답 성공, DTO 필드, 인증·권한, DB 데이터 정합성은 이번 문서 최신화에서 재실행하지 않음
- 비회원 비밀번호 재설정 `API-AUTH-004`, `API-AUTH-005`는 최종 API 명세의 제외 API이므로 집계하지 않음
- 3.1~3.8 분류는 API ID의 `ADMIN` 접두사보다 WBS 기능 도메인을 우선함. 따라서 관리자 컬렉션은 3.2, 관리자 챌린지·쿠폰은 3.6, 관리자 희망도서·공지는 3.7에 배치함
- PR #55의 희망도서·알림·공지 10개 API는 Controller 매핑을, PR #53의 EPUB·뷰어는 Controller·도메인·DDL diff를 대조해 재확인함
- Front 연결은 현재 `abc-front/src`의 실제 `/api/v1` 호출, route, 페이지 컴포넌트 존재 여부만 다시 확인했으며, placeholder 화면은 API 연동 완료로 보지 않음

## 2. 전체 요약

| 구분 | API 수 | 백엔드 구현 O | 프론트 연결 O |
|---|---:|---:|---:|
| 1. 인증/회원 API | 8 | 7 | 7 |
| 2. 도서/카테고리/컬렉션 API | 14 | 14 | 6 |
| 3. 대여/결제 API | 5 | 5 | 5 |
| 4. 전자책 뷰어/북마크/진행 API | 5 | 4 | 0 |
| 5. 리뷰/별점/즐겨찾기 API | 10 | 7 | 7 |
| 6. 챌린지/출석/포인트/쿠폰 API | 12 | 12 | 12 |
| 7. 희망도서/알림/공지 API | 10 | 10 | 0 |
| 8. 관리자 API | 21 | 12 | 4 |
| **합계** | **85** | **71** | **41** |

화면 존재와 해당 API 연결 여부는 별개이므로 각 API 행에서 따로 표시한다. 이번 최신화 범위에서는 U-011, U-019, U-022, U-023의 route·페이지를 찾지 못했으며, A-011·A-012는 route와 placeholder 컴포넌트만 존재한다.

## 3. 도메인별 현황

### 3.1 인증/회원 API (가입/로그인/찾기/재설정)

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 인증 | API-AUTH-001 | O | O | U-002 | O |
| 인증 | API-AUTH-002 | O | O | U-003 | O |
| 인증 | API-AUTH-003 | O | O | U-004 | O |
| 인증 (명세 이후 추가) | API-AUTH-006 | O | O | U-002 | O |
| 회원 | API-ME-001 | O | O | U-014, U-015 | U-014 O, U-015 O |
| 회원 | API-ME-002 | O | O | U-015 | O |
| 회원 | API-ME-003 | O | O | U-015 | O |
| 회원 | API-ME-004 | X | X | U-015 | O |

### 3.2 도서/카테고리/컬렉션 API

| 도메인     | API ID                   | 백엔드 구현 | 프론트 연결 | 화면 ID               | 화면 구현                     |
| ------- | ------------------------ | :----: | :----: | ------------------- | ------------------------- |
| 도서      | API-BOOK-001             |   O    |   O    | U-001, U-006        | U-001 O, U-006 O          |
| 도서      | API-BOOK-002             |   O    |   O    | U-007               | O                         |
| 도서      | API-BOOK-003             |   O    |   O    | U-008               | O                         |
| 도서      | API-BOOK-005             |   O    |   O    | U-001, U-008, U-025 | U-001 O, U-008 O, U-025 O |
| 카테고리    | API-CATEGORY-001         |   O    |   O    | U-001, U-006        | U-001 O, U-006 O          |
| 컬렉션     | API-COLLECTION-001       |   O    |   X    | U-001, U-006        | U-001 O, U-006 O          |
| 컬렉션     | API-COLLECTION-002       |   O    |   X    | U-001, U-006        | U-001 O, U-006 O          |
| 도서관     | API-LIBRARY-001          |   O    |   O    | U-024               | O                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-001 |   O    |   X    | A-018               | O                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-002 |   O    |   X    | A-018               | O                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-003 |   O    |   X    | A-018               | O                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-004 |   O    |   X    | A-018               | O                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-005 |   O    |   -    | -                   | -                         |
| 관리자 컬렉션 | API-ADMIN-COLLECTION-006 |   O    |   -    | -                   | -                         |

#### 3.2.1 관리자 컬렉션 백엔드 최신 구현 기준 (2026-07-16)

프런트 연결·화면 구현 열은 기존 판정을 유지하고, 아래는 현재 `AdminCollectionController` 소스에 존재하는 백엔드 매핑만 최신화한 결과다. `005`·`006`은 API 명세 원문이 아닌 현재 구현 기준으로 부여한 ID다.

| 실제 API ID | Method / URL | 백엔드 역할 | 구현 |
|---|---|---|:---:|
| API-ADMIN-COLLECTION-001 | `GET /api/v1/admin/collections` | 관리자 목록 조회 | O |
| API-ADMIN-COLLECTION-002 | `PUT /api/v1/admin/collections/{collectionId}` | 기존 컬렉션 기본 정보 수정 | O |
| API-ADMIN-COLLECTION-003 | `PUT /api/v1/admin/collections/{collectionId}/books` | 도서 추가·정렬 | O |
| API-ADMIN-COLLECTION-004 | `PATCH /api/v1/admin/collections/{collectionId}` | 상태 변경·도서 제외 | O |
| API-ADMIN-COLLECTION-005 | `POST /api/v1/admin/collections` | 신규 컬렉션 생성 (`201 Created`) | O |
| API-ADMIN-COLLECTION-006 | `GET /api/v1/admin/collections/{collectionId}` | 관리자 상세 조회 | O |

### 3.3 대여/결제 API

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 대여 | API-RENTAL-001 | O | O | U-009 | O |
| 대여 | API-RENTAL-003 | O | O | U-010, U-014 | U-010 O, U-014 O |
| 독서 | API-READING-001 | O | O | U-014, U-028 | U-014 O, U-028 O |
| 결제 | API-PAYMENT-001 | O | O | U-009 | O |
| 결제 | API-PAYMENT-003 | O | O | U-018, U-014 | U-018 O, U-014 O |

### 3.4 전자책 뷰어/북마크/진행 API

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 뷰어 | API-VIEWER-001 | X | X | U-011 | X |
| 진행 | API-VIEWER-002 | O | X | U-011 | X |
| 북마크 | API-VIEWER-003 | O | X | U-011 | X |
| 북마크 | API-VIEWER-004 | O | X | U-011 | X |
| 북마크 | API-VIEWER-005 | O | X | U-011 | X |

#### 3.4.1 EPUB 뷰어 전환 및 명세 외 추가 API (PR #53)

- `API-VIEWER-001`의 명세 URL `GET /api/v1/rentals/{rentalId}/pages/{pageNo}`는 PR #53에서 제거됐다. 현재 구현은 `GET /api/v1/rentals/{rentalId}/viewer` 세션 조회와 `GET /api/v1/rentals/{rentalId}/epub` EPUB 스트리밍으로 전환됐으므로, 현 명세 기준 백엔드 구현은 `X`로 판정한다.
- `PUT /api/v1/rentals/{rentalId}/progress`와 북마크 3개 API는 Controller 매핑이 유지돼 백엔드 `O`다. 다만 U-011 route·페이지·API 호출은 현재 Front에서 찾지 못해 프론트 연결과 화면 구현은 `X`다.
- 신규 관리자 EPUB 업로드 `PUT /api/v1/admin/books/{bookId}/epub`는 아직 API ID가 부여되지 않은 명세 외 API다. `book_epub` 테이블, `reading_progress`의 EPUB 위치·버전·revision 필드, bookmark 위치 필드와 함께 PR #53에서 추가됐다. 전체 API 수와 구현 집계에는 포함하지 않는다.

### 3.5 리뷰/별점/즐겨찾기 API

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 리뷰 | API-REVIEW-001 | O | O | U-008 | O |
| 리뷰/별점 | API-REVIEW-002 | O | O | U-013 | O (U-008 내부 영역) |
| 리뷰 | API-REVIEW-003 | O | O | U-013 | O (U-008 내부 영역) |
| 리뷰 | API-REVIEW-004 | O | O | U-008, U-013 | U-008 O, U-013 O (내부 영역) |
| 즐겨찾기 | API-FAV-001 | O | O | U-001, U-006, U-008 | U-001 O, U-006 O, U-008 O |
| 즐겨찾기 | API-FAV-002 | O | O | U-001, U-006, U-008, U-012 | U-001 O, U-006 O, U-008 O, U-012 O |
| 즐겨찾기 | API-FAV-003 | O | O | U-012, U-014 | U-012 O, U-014 O |
| 신고 | API-REPORT-001 | X | X | U-008 | O |
| 신고 | API-REPORT-002 | X | X | U-008 | O |
| 신고 | API-REPORT-004 | X | X | U-021 | O |

### 3.6 챌린지/출석/포인트/쿠폰 API

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 출석 | API-ATTEND-001 | O | O | U-016 | O |
| 출석 | API-ATTEND-002 | O | O | U-016 | O |
| 포인트 | API-POINT-001 | O | O | U-017 | O |
| 쿠폰 | API-COUPON-001 | O | O | U-009, U-017 | U-009 O, U-017 O |
| 사용자 통계 (독서 통계) | API-STAT-001 | O | O | U-026 | O |
| 챌린지 | API-CHALLENGE-001 | O | O | U-027 | O |
| 챌린지 | API-CHALLENGE-002 | O | O | U-027 | O |
| 관리자 쿠폰 | API-ADMIN-COUPON-001 | O | O | A-013 | O |
| 관리자 쿠폰 | API-ADMIN-COUPON-002 | O | O | A-013 | O |
| 관리자 쿠폰 | API-ADMIN-COUPON-003 | O | O | A-013 | O |
| 관리자 챌린지 | API-ADMIN-CHALLENGE-001 | O | O | A-014 | O |
| 관리자 챌린지 | API-ADMIN-CHALLENGE-002 | O | O | A-014 | O |

### 3.7 희망도서/알림/공지 API

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 공지 | API-NOTICE-001 | O | X | U-001, U-019 | U-001 O, U-019 X |
| 알림 | API-NOTI-001 | O | X | U-019 | X |
| 알림 | API-NOTI-002 | O | X | U-019 | X |
| 희망도서 | API-REQUEST-001 | O | X | U-022 | X |
| 희망도서 | API-REQUEST-002 | O | X | U-023 | X |
| 관리자 희망도서 | API-ADMIN-REQUEST-001 | O | X | A-011 | O (placeholder) |
| 관리자 희망도서 | API-ADMIN-REQUEST-002 | O | X | A-011, A-005 | A-011 O (placeholder), A-005 O |
| 관리자 공지 | API-ADMIN-NOTICE-001 | O | X | A-012 | O (placeholder) |
| 관리자 공지 | API-ADMIN-NOTICE-002 | O | X | A-012 | O (placeholder) |
| 관리자 공지 | API-ADMIN-NOTICE-003 | O | X | A-012 | O (placeholder) |

### 3.8 관리자 API (대시보드/통계/감사로그 등)

| 도메인 | API ID | 백엔드 구현 | 프론트 연결 | 화면 ID | 화면 구현 |
|---|---|:---:|:---:|---|---|
| 관리자 대시보드 | API-ADMIN-DASHBOARD-001 | O | O | A-001 | O |
| 관리자 도서 | API-ADMIN-BOOK-001 | O | X | A-004 | O |
| 관리자 도서 | API-ADMIN-BOOK-002 | O | X | A-005 | O |
| 관리자 도서 | API-ADMIN-BOOK-003 | O | X | A-006 | O |
| 관리자 도서 | API-ADMIN-BOOK-004 | O | X | A-004, A-006 | A-004 O, A-006 O |
| 관리자 도서 | API-ADMIN-BOOK-005 (신규, PR #52) | O | X | A-006 | O |
| 관리자 카테고리 | API-ADMIN-CATEGORY-001 | X | X | A-007 | O |
| 관리자 카테고리 | API-ADMIN-CATEGORY-002 | X | X | A-007 | O |
| 관리자 회원 | API-ADMIN-MEMBER-001 | O | X | A-002 | O |
| 관리자 회원 | API-ADMIN-MEMBER-002 | O | X | A-003 | O |
| 관리자 회원 | API-ADMIN-MEMBER-003 | O | X | A-002, A-003 | A-002 O, A-003 O |
| 관리자 회원 | API-ADMIN-MEMBER-004 | O | O | A-003, A-013 | A-003 O, A-013 O |
| 관리자 대여 | API-ADMIN-RENTAL-001 | X | X | A-008 | O |
| 관리자 결제 | API-ADMIN-PAYMENT-001 | X | X | A-009 | O |
| 관리자 신고 | API-ADMIN-REPORT-001 | X | X | A-010 | O |
| 관리자 신고 | API-ADMIN-REPORT-002 | X | X | A-010 | O |
| 관리자 도서관 | API-ADMIN-LIBRARY-001 | X | X | A-015 | O |
| 관리자 도서관 | API-ADMIN-LIBRARY-002 | X | X | A-015 | O |
| 관리자 도서관 | API-ADMIN-LIBRARY-003 | X | X | A-015 | O |
| 관리자 통계 | API-ADMIN-STAT-001 | O | O | A-016 | O |
| 관리자 감사 로그 | API-ADMIN-AUDIT-001 | O | O | A-017 | O |

## 4. 정적 판정 결과

### 4.1 백엔드 구현 X API

- `API-ME-004`
- `API-VIEWER-001` (PR #53에서 기존 명세 URL 제거)
- `API-REPORT-001`, `API-REPORT-002`, `API-REPORT-004`
- `API-ADMIN-*` 전체 37개 중 대시보드 1개, 관리자 도서 5개, 관리자 회원 4개, 관리자 컬렉션 6개, 쿠폰 3개, 챌린지 2개, 희망도서 2개, 공지 3개, 통계 1개, 감사 로그 1개는 백엔드 구현 `O`이며 나머지 9개는 `X`
- 관리자 컬렉션 6개는 3.2에, 관리자 희망도서·공지 5개는 3.7에 배치한다.

### 4.2 백엔드는 구현됐으나 Front 호출 연결이 확인되지 않은 API

- `API-COLLECTION-001`
- `API-COLLECTION-002`
- `API-VIEWER-002`, `API-VIEWER-003`, `API-VIEWER-004`, `API-VIEWER-005`
- `API-NOTICE-001`, `API-NOTI-001`, `API-NOTI-002`
- `API-REQUEST-001`, `API-REQUEST-002`
- `API-ADMIN-REQUEST-001`, `API-ADMIN-REQUEST-002`
- `API-ADMIN-NOTICE-001`, `API-ADMIN-NOTICE-002`, `API-ADMIN-NOTICE-003`
- `API-ADMIN-BOOK-001`, `API-ADMIN-BOOK-002`, `API-ADMIN-BOOK-003`, `API-ADMIN-BOOK-004`, `API-ADMIN-BOOK-005`

### 4.3 화면 구현 현황

- 이번 최신화 대상 User 화면 중 U-011, U-019, U-022, U-023은 `abc-front/src/app/router.tsx`와 `pages/user`에서 route·페이지를 찾지 못해 `X`
- A-011·A-012는 route와 컴포넌트가 존재하지만 placeholder이므로, 화면 표기에는 `placeholder`를 병기하고 프론트 연결은 `X`
- `U-013`은 별도 route가 아니라 `U-008` 도서 상세 내부 리뷰 영역으로 구현
- 이 범위 밖 화면의 기존 정적 판정은 재조사하지 않음

### 4.4 담당 도메인 계획 문서 기준 재검증

| 담당 기능 범위 | 확인 대상 | 문서 기준 판정 | 해석 |
|---|---|---|---|
| 도서/카테고리/컬렉션 | 3.2의 사용자 API 8개 | 백엔드 8/8 `O` | 사용자 도서·카테고리·컬렉션·도서관 API는 구현 기록 확인 |
| 관리자 컬렉션 | `API-ADMIN-COLLECTION-001`~`006` | 백엔드 6/6 `O` | `AdminCollectionController`의 실제 6개 매핑과 API-002 `updatedAt` 보완·Swagger 재검증 결과를 확인 |
| 관리자 도서 | `API-ADMIN-BOOK-001`~`005` | 백엔드 5/5 `O` | PR #52 최종 병합 내용 기준 목록·등록·수정·상태 변경·상세 조회 구현 완료 |
| 전자책 뷰어/북마크/진행 | `API-VIEWER-001`~`005` | 백엔드 4/5 `O`, 프론트 0/5 `O` | PR #53에서 EPUB 전환. `API-VIEWER-001`의 기존 명세 URL은 제거돼 API 명세 개정 전까지 `X`; 진행·북마크 매핑은 유지 |
| 희망도서/알림/공지 | 3.7의 10개 API | 백엔드 10/10 `O`, 프론트 0/10 `O` | PR #55 Controller 매핑으로 백엔드 구현 확인. 현재 Front API 호출부는 없음 |
| 챌린지/출석/포인트/쿠폰 | 3.6의 12개 | 백엔드 12/12 `O` | 사용자 API와 관리자 쿠폰·챌린지 API 구현·테스트 기록 확인 |
| 관리자 핵심 + 사용자 통계 | `API-ADMIN-DASHBOARD-001`, `API-ADMIN-STAT-001`, `API-ADMIN-AUDIT-001`, `API-STAT-001` | 백엔드 4/4 `O` | 담당 도메인 문서에 4개 endpoint 구현 완료로 명시됨 |
| 로그인 아이디 중복 확인 | `API-AUTH-006` | 백엔드 `O`, 프론트 연결 `O` | 최종 API 명세 이후 추가. `GET /api/v1/auth/check-login-id`, 회원가입 `U-002`에서 사용 |

따라서 담당 범위가 “관리자 접두사가 붙은 모든 API”라는 뜻은 아니다. WBS 기능 기준으로 보면 관리자 컬렉션은 3.2, 관리자 쿠폰·챌린지는 3.6, 관리자 희망도서·공지는 3.7에 속한다. 반대로 3.8은 이동 후 남은 관리자 기능을 모은 구분이며, 그 21개 전체를 한 사람의 담당 범위로 해석하지 않는다.

## 5. 확인 근거

- API ID/Method/URL/화면 ID: `docs/API 명세/api-spec(final).md` 162~249행
- 제외 API: 같은 문서 379~386행
- 기존 백엔드·Front·화면 판정: 이 문서 최초 작성 시 수행한 정적 매핑 조사 결과
- 도서/카테고리/컬렉션: `docs/작업현황/2026-07-15_도서-카테고리-컬렉션API_코드리뷰_및_재검증계획.md`, `docs/작업현황/0702/2026-07-02_카탈로그API_변경이력.md`
- 관리자 컬렉션 백엔드 최신화: `src/main/java/com/acorn/abc/admin/collection/controller/AdminCollectionController.java`, `src/main/java/com/acorn/abc/collection/service/AdminCollectionService.java`, `api-test-results/관리자 컬렉션 API/코드 수정 후 재시도/swagger/200.json`
- 뷰어/북마크/진행: `docs/작업현황/0702/2026-07-02_뷰어-북마크-진행API_구현계획.md`, `docs/작업현황/0702/2026-07-02_카탈로그API_변경이력.md`
- 챌린지/출석/포인트/쿠폰: `docs/작업현황/0706/2026-07-06_챌린지-출석-포인트-쿠폰API_테스트시나리오_체크리스트.md`, `docs/작업현황/0707/2026-07-07_챌린지출석포인트쿠폰API_실패항목_Swagger_재테스트_계획.md`
- 관리자 핵심·사용자 통계: `docs/작업현황/0713/2026-07-13_관리자API+사용자통계_전체잔여작업_구현계획.md`
- 관리자 도서: GitHub PR #52 최종 병합 내용 및 후속 리뷰 기록 — `API-ADMIN-BOOK-001`~`005` 구현, 빌드 522 tests 성공 및 Swagger 수동 검증 통과 기록
- EPUB·뷰어: GitHub PR #53 병합 diff — `AdminBookEpubController`, `ViewerController`, `BookEpub`, `src/main/resources/schema.sql`, `src/main/resources/db/manual/V001__epub_additive.sql`
- 희망도서·알림·공지: GitHub PR #55 병합 diff — `BookRequestController`, `MyBookRequestController`, `NotificationController`, `NoticeController`, `AdminBookRequestController`, `AdminNoticeController`, `src/main/resources/db/manual/V002__request_notification_notice_alignment.sql`
- 신규 로그인 아이디 중복 확인: `docs/작업현황/2026-07-15_PR49_코드리뷰_후속수정계획.md`, `docs/작업현황/설계문서/2026-07-15_PR49_API-AUTH-006_설계문서_반영예정.md`

## 6. 제한사항

- 이 문서는 정적 구현 여부를 빠르게 파악하기 위한 1차 현황표이다.
- PR #55는 PR 본문에서 580개 테스트, migration 왕복, HTTP smoke test 성공을 주장하지만 이번 문서 최신화에서는 재실행하지 않았다.
- PR #53은 EPUB 관련 테스트가 추가됐지만 커밋 기록상 전체 테스트는 당시 로컬 DB drift로 실패했다. 이번 문서에는 PR diff 확인과 독립 검증 완료를 구분해 기록했다.
- `API-VIEWER-001`은 구현 URL이 API 명세와 달라, API 명세 개정 또는 별도 API ID 부여 전에는 현재 판정을 유지해야 한다.
- 관리자 도서 5개는 PR #52의 최종 병합 diff와 성공 기록을 기준으로 백엔드 구현 `O`로 반영했다. 이번 문서 최신화에서는 빌드·단위 테스트·Swagger를 다시 실행하지 않았다.
- 실제 응답 성공 여부, 요청·응답 DTO 일치, 인증·권한, CORS, 환경 변수, DB seed와 데이터 상태는 후속 통합 테스트에서 별도로 확인해야 한다.
- Front에 wrapper가 있어도 백엔드 Controller 매핑이 없으면 프론트 연결은 `X`로 판정했다.
