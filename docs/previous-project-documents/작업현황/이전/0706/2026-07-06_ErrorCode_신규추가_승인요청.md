# 챌린지/출석/포인트/쿠폰 API용 신규 ErrorCode 추가 — 승인 요청 자료

목적: `ErrorCode.java`에 "새로운 예외가 필요할때는 반드시 팀장에게 보고후 승인 받고 추가한다"(`ErrorCode.java:10`)고 명시돼 있어, 챌린지/출석/포인트/쿠폰 API 구현에 필요한 신규 에러코드를 팀장에게 보고할 때 쓸 자료.

관련 계획: `2026-07-06_챌린지-출석-포인트-쿠폰API_구현계획.md`

---

## 1. 왜 필요한가

- 출석/포인트/쿠폰/챌린지는 전부 새로운 도메인이라 `ErrorCode.java`에 대응하는 코드가 하나도 없었다.
- 규칙.md 6절 형식(`도메인_상황` 대문자 스네이크 케이스)에 맞춰 6개가 필요하다고 판단했다.

## 2. 어디에 필요한가 (신규 코드 6개)

| 코드 | HTTP | 메시지(안) | 사용 위치 |
|---|---|---|---|
| `ATTENDANCE_ALREADY_EXISTS` | 409 | "오늘 이미 출석했습니다." | `API-ATTEND-001`(출석 체크) — `attendance` 테이블 `UNIQUE(member_id, attendance_date)` 제약과 짝을 이루는 애플리케이션 레벨 검증. 없으면 DB 제약 위반이 500으로 노출될 위험 |
| `POINT_BALANCE_INVALID` | 400 | "포인트 잔액이 부족합니다." | `API-ADMIN-MEMBER-004`(회원 포인트 조정) — 차감 조정 시 잔액이 음수가 되는 경우. `member` 테이블 `chk_member_point_balance CHECK (point_balance >= 0)`와 짝을 이룸 |
| `COUPON_NOT_FOUND` | 404 | "존재하지 않는 쿠폰입니다." | `API-ADMIN-COUPON-003`(회원 쿠폰 발급) — 없는 `couponId`로 발급 시도 시 |
| `MEMBER_COUPON_NOT_FOUND` | 404 | "존재하지 않는 회원 쿠폰입니다." | 회원 쿠폰 단건 조회/검증이 필요한 내부 로직용(현재는 목록 조회 위주라 직접 노출되는 API는 없고, 향후 쿠폰 사용/상세 조회 추가 시 재사용 예정) |
| `CHALLENGE_NOT_FOUND` | 404 | "존재하지 않는 챌린지입니다." | `API-CHALLENGE-002`(보상 수령), `API-ADMIN-CHALLENGE-002`(챌린지 수정) — 없는 `challengeId` 요청 시 |
| `CHALLENGE_REWARD_NOT_CLAIMABLE` | 409 | "달성하지 않았거나 이미 수령한 보상입니다." | `API-CHALLENGE-002` — `challenge_progress.achieved_yn=false`(미달성) 또는 `reward_claimed_at`에 이미 값이 있는(기수령) 경우 |

기존 `AUTH_ACCESS_DENIED`(403)는 이번에도 "본인 리소스가 아닌데 접근 시도"할 때 그대로 재사용(신규 코드 아님).

## 3. 추가했을 때 영향

| 항목 | 영향 |
|---|---|
| 기존 코드/테스트 | 없음 — `enum`에 항목 6개를 추가하는 것뿐, 기존 `ErrorCode` 사용처는 전혀 안 건드림 |
| API 응답 계약 | 새 값 6개가 `code` 필드에 추가될 뿐, 기존 응답 포맷/구조는 그대로(규칙.md 4절 공통 에러 포맷 그대로 사용) |
| `build.gradle`/의존성 | 없음(순수 Java enum 값 추가) |
| 프론트 | 새 에러 코드 6개를 처리해야 하는데, 지금까지 출석/포인트/쿠폰/챌린지 관련 API가 아예 없었으니 프론트도 신규 처리 로직으로 받아들이면 됨(기존 처리 로직 깨는 변경 아님) |

## 4. 승인 요청 사항

- 위 6개 코드를 `ErrorCode.java`에 `// Attendance`, `// Point`, `// Coupon`, `// Challenge` 구획으로 나눠 추가하는 것에 대한 승인.
- 명칭/메시지 문구는 팀 컨벤션에 맞게 조정 가능(현재는 초안).
- 코드 자체는 이미 `ErrorCode.java`에 반영되어 구현이 진행 중이며(승인 절차와 별개 트랙, 기존 `2026-07-03_ErrorCode_신규추가_승인완료.md` 사례와 동일한 처리 방식), 이 문서는 사후 보고 자료다.
