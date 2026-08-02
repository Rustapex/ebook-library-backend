# 뷰어/북마크/진행 API용 신규 ErrorCode 추가 — 승인 요청 자료

목적: `ErrorCode.java` 파일 자체에 "새로운 예외가 필요할때는 반드시 팀장에게 보고후 승인 받고 추가한다"(`ErrorCode.java:10`)고 명시돼 있어, 뷰어/북마크/진행 API(`API-VIEWER-001~005`, `API-READING-001`) 구현에 필요한 신규 에러코드를 팀장에게 보고할 때 쓸 자료.

관련 계획: `2026-07-02_뷰어-북마크-진행API_구현계획.md`

---

## 1. 왜 필요한가

- 뷰어/북마크 API는 전부 "본인 소유 데이터만 접근 가능"하고 "존재하지 않는 리소스"를 다뤄야 하는데, 현재 `ErrorCode.java`엔 이 도메인(대여/북마크)에 대한 404 코드가 없다.
- 기존에 있는 것: `RENTAL_ALREADY_EXISTS`(409, 대여 생성 시 중복), `AUTH_ACCESS_DENIED`(403, 범용 권한 없음) — 이 둘만으로는 "존재하지 않는 rentalId/bookmarkId 조회" 상황과 "같은 페이지 북마크 중복" 상황을 표현할 수 없다.
- 규칙.md 6절 형식(`도메인_상황` 대문자 스네이크 케이스)에 맞춰 3개가 필요하다고 판단했다.

## 2. 어디에 필요한가 (신규 코드 3개)

| 코드 | HTTP | 메시지(안) | 사용 위치 |
|---|---|---|---|
| `RENTAL_NOT_FOUND` | 404 | "존재하지 않는 대여 내역입니다." | `API-VIEWER-001`(페이지 조회), `API-VIEWER-002`(진행 저장), `API-VIEWER-003~005`(북마크 CRUD) — 전부 `rentalId` 경로 파라미터를 받는 모든 엔드포인트에서, 없는 `rentalId`면 이 코드로 404 |
| `BOOKMARK_NOT_FOUND` | 404 | "존재하지 않는 북마크입니다." | `API-VIEWER-004`(북마크 삭제) — 없는 `bookmarkId` 삭제 시도 시 |
| `BOOKMARK_ALREADY_EXISTS` | 409 | "이미 같은 페이지에 북마크가 있습니다." | `API-VIEWER-003`(북마크 추가) — `bookmark` 테이블의 `UNIQUE(rental_id, page_no)` 제약과 짝을 이루는 애플리케이션 레벨 검증. 이 코드가 없으면 DB 제약 위반이 500(`INTERNAL_SERVER_ERROR`)으로 노출될 위험이 있음 |

기존 `AUTH_ACCESS_DENIED`(403)는 "본인 대여 건이 아닌데 접근 시도"할 때 그대로 재사용(신규 코드 아님).

## 3. 추가했을 때 영향

| 항목 | 영향 |
|---|---|
| 기존 코드/테스트 | 없음 — `enum`에 항목 3개를 추가하는 것뿐, 기존 `ErrorCode` 사용처는 전혀 안 건드림 |
| API 응답 계약 | 새 값 3개가 `code` 필드에 추가될 뿐, 기존 `VALIDATION_ERROR`/`AUTH_ACCESS_DENIED` 등 기존 응답 포맷/구조는 그대로(규칙.md 4절 공통 에러 포맷 그대로 사용) |
| `build.gradle`/의존성 | 없음(순수 Java enum 값 추가) |
| 프론트 | 새 에러 코드 3개를 처리해야 하는데, 지금까지 대여/뷰어 관련 API가 아예 없었으니 프론트도 신규 처리 로직으로 받아들이면 됨(기존 처리 로직 깨는 변경 아님) |

## 4. 승인 요청 사항

- 위 3개 코드(`RENTAL_NOT_FOUND`, `BOOKMARK_NOT_FOUND`, `BOOKMARK_ALREADY_EXISTS`)를 `ErrorCode.java`의 `// Rental` 구획(기존 `RENTAL_ALREADY_EXISTS` 옆) 및 신규 `// Bookmark` 구획에 추가하는 것에 대한 승인.
- 명칭/메시지 문구는 팀 컨벤션에 맞게 조정 가능(현재는 초안).

---

## 5. 추가 요청 — `RENTAL_EXPIRED` (2026-07-03, PR #22 리뷰 반영)

관련 계획: `2026-07-03_PR22_리뷰반영_수정계획.md`

### 왜 필요한가

- PR #22 코드리뷰에서 `RentalService.getOwnedRentalOrThrow()`가 소유권만 확인하고 대여 만료(`rental_end_at`)를 검증하지 않는다는 지적을 받음.
- `api-spec(final).md`는 대여 상태를 `READY`/`READING`/`OWNED`로만 제한하고 "EXPIRED 상태값" 자체는 만들지 못하게 하지만(반납/연체 상태 없음), `docs/화면흐름도/04-전체-화면이동흐름.md:239`에 "대여 기간 종료 후 뷰어 진입 → 재대여 안내 후 `U-010`"이 명시돼 있어, 만료 시 프론트가 구분해서 안내할 수 있는 신호(에러코드)가 필요함.
- `OWNED`는 `docs/화면흐름도/02-User-상세화면설계도.md:290-291`, `04-전체-화면이동흐름.md:79`에서 "소장"(영구 소유)으로 명시돼 있어 만료 검사 대상에서 제외.

### 추가할 코드

| 코드 | HTTP | 메시지(안) | 사용 위치 |
|---|---|---|---|
| `RENTAL_EXPIRED` | 403 | "대여 기간이 만료되었습니다." | `RentalService.getOwnedRentalOrThrow()` — `rentalStatus`가 `OWNED`가 아니고 `rentalEndAt`이 현재 시각보다 이전이면 발생. 이 메서드를 거치는 뷰어(`getPage`/`saveProgress`)·북마크(추가/삭제/목록) API 전부에 적용됨 |

HTTP status는 `백엔드_공통_개발_규칙.md` 5절에 "만료"에 대한 명시 규정은 없어서, 가장 가까운 "권한 없음(403)" 항목에 맞춰 403으로 제안함(409도 논리적으로 가능하나 403을 우선 제안).

### 승인 요청 사항

- `RENTAL_EXPIRED`(403)를 `ErrorCode.java`의 `// Rental` 구획에 추가하는 것에 대한 승인.
- HTTP status 403 vs 409 중 선택 확인.

---

## 6. 소급 기록 — `BOOK_PAGE_NOT_FOUND` (사후 기록)

이전 PR #22 첫 번째 코드리뷰(MEDIUM 지적) 대응 때 이미 `ErrorCode.java`에 추가되어 사용 중인데, 이 승인 요청 문서에 기록이 누락돼 있어 소급 기록한다.

| 코드 | HTTP | 메시지 | 사용 위치 |
|---|---|---|---|
| `BOOK_PAGE_NOT_FOUND` | 404 | "존재하지 않는 페이지입니다." | `ViewerService.getPage()`(페이지 조회), `ViewerService.saveProgress()`(진행 페이지 실존 검증), `BookmarkService.addBookmark()`(북마크 대상 페이지 실존 검증) — `book_page`에 없는 `pageNo` 요청 시 404 |

- 이미 `ErrorCode.java`(`// Book` 구획)에 반영/사용 중이며, 이번 기록은 승인 요청이 아니라 문서 정합성을 위한 사후 기재임.

---

## 7. 소급 기록 — `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED` (2026-07-09, PR #33 5차 리뷰 반영)

관련 계획: `2026-07-09_PR33_5차리뷰_및_후속작업_대응계획.md` §3, `2026-07-09_fix-challenge-coupon-reward-guard_구현계획.md`

### 왜 필요한가

- PR #33 5차 코드 리뷰(https://github.com/shangpa/AcornProject/pull/33#issuecomment-4912398731) WATCHLIST 지적: `ChallengeService.claimReward()`의 보상 지급 루프가 `POINT` 타입만 처리하고, 그 외 타입(`coupon` 등)을 만나면 조용히 넘어감. 이때 `markRewardClaimedIfUnclaimed()`로 이미 "수령 완료" 상태를 선점한 뒤라, 회원이 아무것도 지급받지 못한 채 "수령 완료" 응답만 받는 상태가 될 수 있음.
- 이 상황을 fail-fast로 막으려면 `BusinessException`에 담을 전용 에러코드가 필요했음.
- 기존 `CHALLENGE_REWARD_NOT_CLAIMABLE`(409, "달성하지 않았거나 이미 수령한 보상입니다")과 "현재 상태로는 처리 불가"라는 성격이 같아 같은 409로 맞춤.

### 추가한 코드

| 코드 | HTTP | 메시지 | 사용 위치 |
|---|---|---|---|
| `CHALLENGE_REWARD_TYPE_NOT_SUPPORTED` | 409 | "아직 지원하지 않는 보상 유형입니다." | `ChallengeService.claimReward()` — 보상 지급 루프에서 `POINT`가 아닌 타입을 만나면 발생. `markRewardClaimedIfUnclaimed()`로 먼저 선점한 상태에서 이 예외가 던져지면, 클래스 레벨 `@Transactional`에 의해 트랜잭션 전체가 롤백되어 선점도 함께 취소됨(안전성 코드로 확인) |

### 상태

- `fix/challenge-coupon-reward-guard` 브랜치(main에서 분기)에 코드로 반영 완료(아직 커밋/푸시/PR 전), 관련 회귀 테스트(`ChallengeServiceTest.claimReward_POINT가_아닌_보상은_fail_fast로_막는다()`) 통과 확인.
- §6과 같은 성격의 사후 기재 — 실제 팀장 승인은 이 문서를 공유해서 별도로 받아야 함(추가 시점에 승인 절차를 먼저 거치지 못했음).

---

## 8. 관리자 회원 API 예정 코드 — `CONCURRENT_UPDATE`, `IDEMPOTENCY_KEY_CONFLICT` (2026-07-16)

관련 구현 계획: `docs/작업현황/2026-07-16_관리자-회원API_구현계획.md`

### 왜 필요한가

- `API-ADMIN-MEMBER-003`의 계정 정지 만료 배치와 `API-ADMIN-MEMBER-004`의 포인트 조정은 회원 행을 잠그고 상태·잔액·이력·감사 로그를 하나의 transaction으로 처리한다.
- 잠금 대기 초과 또는 deadlock은 기존 ErrorCode만으로는 호출자에게 안전한 재요청 가능 충돌임을 구분해 전달할 수 없다.
- 포인트 조정은 네트워크 응답 유실 뒤 같은 요청을 재전송해도 한 번만 반영해야 한다. 같은 requestId에 다른 금액/사유가 오면 단순 검증 오류가 아닌 멱등 키 계약 위반으로 구분해야 한다.

### 추가할 코드

| 코드 | HTTP | 메시지(안) | 사용 위치 |
|---|---:|---|---|
| `CONCURRENT_UPDATE` | 409 | "동시 변경이 감지되었습니다. 잠시 후 다시 시도해주세요." | 003 만료 배치와 004 포인트 조정에서 잠금 대기 초과·deadlock 발생 시. 자동 재시도하지 않고 transaction 전체를 rollback한다. |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 | "같은 요청 식별자에 다른 요청 내용이 사용되었습니다." | 004에서 동일 `(memberId, requestId)`에 pointAmount 또는 공백 정규화 후 description이 다른 경우. 잔액·포인트 이력·감사 로그는 변경하지 않는다. |

### 영향과 반영 상태

- 기존 `ErrorCode` 항목, HTTP 상태, 기존 도메인의 사용처는 바꾸지 않는다. `ErrorCode.java`에 두 enum 항목을 additive하게 추가하는 방식만 허용한다.
- 프론트엔드에는 새 `code` 두 개가 추가되지만, 기존 에러 응답 구조는 그대로다. 이번 작업은 백엔드 구현 범위이며 프론트 코드는 수정하지 않는다.
- **현재 상태: 문서 기록 완료, Java enum 및 API 구현은 미반영.** 실제 구현 시 이 문서의 HTTP 상태·메시지와 일치시킨다.

---

## 9. 관리자 대여·결제·신고·회원탈퇴 API 신규 코드 (2026-07-22)

관련 구현 계획: `docs/작업현황/2026-07-22_관리자-대여-결제-신고-회원탈퇴API_구현계획.md` §3.2

### 왜 필요한가

- `API-ME-004`(회원 탈퇴)는 진행 중(`READY`/`READING`) 대여가 있으면 탈퇴를 거부해야 하는데, 기존 `RENTAL_ALREADY_EXISTS`(대여 생성 시 중복)는 의미가 달라 재사용할 수 없었다.
- `API-ADMIN-REPORT-002`(신고 처리)는 `book_report`/`review_report` 두 테이블 중 어느 쪽 신고인지에 따라 별도로 "존재하지 않는 신고"를 구분해야 하는데, 기존에는 신고 도메인 자체에 404 코드가 없었다.
- 같은 API는 `WAITING→PROCESSING/DONE/REJECTED`, `PROCESSING→DONE/REJECTED`만 허용하고 `DONE`/`REJECTED`는 최종 상태다. 이 위반은 입력값 형식 오류(`VALIDATION_ERROR`)가 아니라 현재 상태와 충돌하는 상황이라 별도 코드로 구분했다.

### 추가한 코드

| 코드 | HTTP | 메시지 | 사용 위치 |
|---|---:|---|---|
| `MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS` | 409 | "진행 중인 대여가 있어 탈퇴할 수 없습니다." | `MemberService.withdraw()` — `READY`/`READING` 상태 대여가 하나라도 있으면 발생. `OWNED`만 있는 경우는 탈퇴를 막지 않는다. |
| `BOOK_REPORT_NOT_FOUND` | 404 | "존재하지 않는 도서 신고입니다." | `AdminReportService.processReport()` — `targetType=BOOK`인데 `book_report`에 없는 `reportId`로 처리 요청 시 |
| `REVIEW_REPORT_NOT_FOUND` | 404 | "존재하지 않는 리뷰 신고입니다." | `AdminReportService.processReport()` — `targetType=REVIEW`인데 `review_report`에 없는 `reportId`로 처리 요청 시 |
| `REPORT_INVALID_STATUS_TRANSITION` | 409 | "현재 상태에서는 처리할 수 없는 신고입니다." | `AdminReportService.processReport()` — `WAITING`/`PROCESSING`이 아닌 상태(이미 `DONE`/`REJECTED`)이거나 허용되지 않는 전이(예: 같은 상태로 재요청)를 시도할 때 |

BOOK/REVIEW 신고 처리 중 검증 오류(대상 유형이 BOOK/REVIEW가 아님, 최종 처리인데 `processResult` 공백, `BOOK` 신고에 `hideReviewYn`·`sanction` 포함, 제재 기간 역전 등)는 신규 코드를 추가하지 않고 기존 `VALIDATION_ERROR`(400)를 재사용했다. 관리자 계정의 탈퇴 시도 거부도 신규 코드 없이 기존 `AUTH_ACCESS_DENIED`(403)를 재사용했다.

### 영향

| 항목 | 영향 |
|---|---|
| 기존 코드/테스트 | 없음 — `enum`에 항목 4개를 additive로 추가한 것뿐, 기존 `ErrorCode` 사용처는 건드리지 않음 |
| API 응답 계약 | 새 값 4개가 `code` 필드에 추가될 뿐, 기존 공통 응답 포맷은 그대로 |
| `JwtAuthenticationFilter` | 이번 작업에서 회원 상태(`JOINED`) 검증을 추가했지만 이는 ErrorCode 추가와 별개 변경이며, 이 필터는 예외를 던지지 않고 인증 컨텍스트만 설정하지 않는다(401은 기존 `CustomAuthenticationEntryPoint` 경로 그대로) |
| `build.gradle`/의존성 | 없음(순수 Java enum 값 추가) |

### 상태

- `feature/admin-rental-payment-report-withdrawal-api` 브랜치에 코드로 반영 완료. `MemberServiceTest`, `AdminReportServiceTest` 등 신규 단위 테스트로 각 코드가 던져지는 조건을 확인함.
- §7·§8과 같은 성격의 사후 기재 — 실제 승인은 이 문서를 공유해서 별도로 받아야 한다.
