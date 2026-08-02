# AGENTS.md CLAUDE 문서 반영 수정계획

작성일: 2026-07-11  
문서유형: 수정계획  
대상 파일: `AGENTS.md`

## 1. 목적

`CLAUDE.md`와 `CLAUDE.local.md`는 `/init`으로 생성된 최신 작업 기준 문서이므로, 현재 `AGENTS.md`의 일반적인 contributor guide를 이 저장소의 실제 운영 규칙에 맞게 보강한다.

이번 단계에서는 `AGENTS.md`를 직접 수정하지 않고, 반영 범위와 제외 범위를 먼저 정리한다.

## 2. 확인한 근거

- `CLAUDE.md:5-12` 프로젝트 개요: Spring Boot 4.x, Java 17, Gradle, React/Vite/TypeScript, 실제 MySQL 사용, ArchUnit 미도입.
- `CLAUDE.md:14-37` Commands: backend `bootRun --console=plain`, `build`, `test`, 단일 테스트 실행, frontend `dev/build/lint/preview`.
- `CLAUDE.md:39-49` Architecture: feature-based backend package, `common/*` 공통 구성, frontend lint source of truth.
- `CLAUDE.md:51-72` 기준 문서와 문서 우선순위: API 명세, 백엔드 공통 규칙, 실제 schema/data 우선.
- `CLAUDE.md:74-84` REST API 작업 시작 규칙: handoff 확인, 관련 문서 대조, 구현 전 계획 문서 작성, ErrorCode 승인 문서, targeted reading.
- `CLAUDE.md:86-105` 구현/검증 규칙: DTO/응답/예외/엔티티 규칙, 범위 제한, 실제 MySQL 검증, Swagger 수동 체크리스트, 실행하지 않은 테스트 보고 금지.
- `CLAUDE.md:107-114` 세션 운영 규칙: 작업현황 문서명, `CLAUDE.local.md` 역할, 커밋/PR 메시지, 브랜치 전략, 승인 필요 액션.
- `CLAUDE.local.md:1-49` 현재 스냅샷: 현재 브랜치/완료 작업/미완료 작업/주의사항. 단, 이는 영구 가이드에 그대로 복사하지 않는다.
- `AGENTS.md:1-37` 현재 문서: 구조, 명령, 스타일, 테스트, 커밋/PR 정도만 간략히 존재한다.

## 3. 현재 AGENTS.md의 부족한 점

- 실제 MySQL 8.x를 테스트와 런타임 모두에 사용한다는 위험 조건이 빠져 있다.
- `docs/공통문서/백엔드_공통_개발_규칙.md`, API 명세, `schema.sql` 등 기준 문서 우선순위가 없다.
- REST API 작업 전 검토 문서/구현계획 문서를 먼저 작성하는 절차가 없다.
- `CLAUDE.local.md`를 현재 상태 스냅샷으로 먼저 확인하라는 규칙이 없다.
- `ErrorCode` 신규 추가 시 승인 요청 문서를 먼저 만드는 규칙이 없다.
- Swagger 수동 검증, DB 백업/원복, 동시성 검증 계획 등 검증 규칙이 부족하다.
- 커밋/PR 메시지의 “type 영어 + 설명 한국어” 규칙과 branch/PR 전략이 약하다.
- 삭제, reset, force push, commit, push, PR 생성, secret 열람 등 승인 필요 액션이 구체적으로 정리되어 있지 않다.

## 4. 수정 방향

### 4.1 문서 성격 유지

- 제목 `Repository Guidelines`는 유지한다.
- `AGENTS.md`는 Codex가 실제 작업할 때 따라야 하는 로컬 운영 규칙 문서로 둔다. 외부 contributor guide처럼 짧고 일반적인 안내문에 머물지 않고, Codex 작업에 필요한 절차·검증·승인 경계까지 포함한다.
- 너무 긴 `CLAUDE.md` 복사본이 되지 않도록 원본 경로와 우선순위를 기록한다.
- `CLAUDE.md`는 Claude용 문서, `AGENTS.md`는 Codex용 문서로 분리한다. Codex는 `AGENTS.md`를 기준으로 작업하므로, `CLAUDE.md`의 유효한 프로젝트 규칙 중 Codex에도 필요한 내용은 `AGENTS.md`에 요약 반영한다.

### 4.2 추가/보강할 섹션

1. `Project Overview & Structure`
   - Spring Boot 4.x backend, React/Vite frontend, feature-based package 구조를 명시한다.
   - `common/response`, `common/exception`, `common/domain`, `common/security`, `common/config` 역할을 요약한다.
   - 일부 도메인은 full vertical slice가 아닐 수 있으므로 entity 존재만으로 controller/service 존재를 가정하지 말라고 적는다.

2. `Source Documents & Priority`
   - API 명세, 백엔드 공통 개발 규칙, 실제 `schema.sql`, `data.sql`, ERD/DDL 설계 문서의 역할을 표로 정리한다.
   - 런타임 `src/main/resources/schema.sql`이 설계 ERD보다 우선하지만, 불일치는 조용히 넘기지 말고 보고하도록 적는다.
   - API 명세와 구현이 다르면 임의로 구현에 맞추지 말고 차이를 보고하도록 한다.

3. `Build, Test, and Development Commands`
   - 현재 명령에 `bootRun --console=plain`, 단일 테스트 클래스/메서드 실행, `npm run preview`를 추가한다.
   - MySQL 8.x DB `ABC`, `.env.example` 기반 `.env`, `spring.sql.init.mode=always` 주의사항을 추가한다.

4. `REST API Work Rules`
   - 작업 시작 시 `CLAUDE.local.md`의 Current handoff를 확인한다.
   - 관련 화면흐름도/API 명세/schema를 targeted reading으로 대조한다.
   - 구현 전 검토/구현계획 문서를 `docs/작업현황`에 먼저 작성한다.
   - “계획” 요청에는 문서 작성까지만 하고 실제 소스는 수정하지 않는다고 명시한다.
   - 새 `ErrorCode`가 필요하면 승인 요청 문서를 먼저 만든다.

5. `Coding Style & Backend Rules`
   - 기존 DTO/응답/예외/엔티티 규칙을 유지하고 `/api/v1`, 복수형 URL, `@Valid`, `BusinessException`, `BaseEntity` 규칙을 보강한다.
   - scope-out 결정은 문서에 명시하고, 무관한 파일은 수정하지 않는다고 적는다.

6. `Testing & Verification`
   - 백엔드 변경 후 `.\gradlew.bat build` 실행을 기본 완료 조건으로 둔다.
   - 테스트는 H2/Testcontainers가 아닌 실제 MySQL 기준임을 명시한다.
   - DB 상태 의존 테스트는 현재 상태 확인, 백업, 임시 조작, 테스트, 원복 순서를 문서화한다.
   - 신규/변경 API는 Swagger 수동 체크리스트와 DB 정리 SQL을 준비한다.
   - ArchUnit 등 자동 컨벤션 검증은 미도입이므로 수동 검증과 구분해 보고한다.
   - 실행하지 않은 테스트는 실행한 것처럼 보고하지 않는다.

7. `Session, Git, and PR Rules`
   - 작업현황 문서명 규칙: `docs/작업현황/YYYY-MM-DD_주제_문서유형.md`.
   - `CLAUDE.local.md`는 로컬 전용 현재 스냅샷이며 과거 이력 누적 문서가 아니라고 적는다.
   - 커밋/PR 메시지는 `feat/fix/docs/refactor/test/chore` type 영어 + 설명 한국어.
   - 브랜치는 `main` + `feature/*`, merge는 PR로만 진행한다.
   - commit/push/PR create/merge/approve/request changes는 명시 지시 전 실행하지 않는다.

8. `Security & Approval Boundaries`
   - secret, `.env`, token, auth 원문을 읽거나 출력하지 않는다.
   - 파일/폴더 삭제, `git clean`, `git reset --hard`, force push, 의존성/빌드 설정 변경은 사전 승인 대상이다.
   - `rg`, `git status`, `git diff`, 테스트/빌드 등 조사와 검증 명령은 일반적으로 자동 진행 가능하다고 정리한다.

## 5. 반영하지 않을 내용

- `CLAUDE.local.md`의 특정 브랜치명, PR 번호, 커밋 해시, 완료/미완료 작업 목록은 현재 세션 스냅샷이므로 `AGENTS.md`에 고정하지 않는다.
- PR #36 검증 코멘트 게시 여부, BaseEntity 정리 착수 여부 같은 현재 작업 항목은 contributor guide가 아니라 handoff 문서에 남긴다.
- `CLAUDE.md`의 기준 문서 표 전체를 장문으로 복사하지 않고, 핵심 경로와 우선순위만 요약한다.
- 오래된 설계 문서를 추가로 읽어 최신 상태로 단정하지 않는다. 필요 시 각 작업 시작 시 관련 범위만 다시 확인한다.

## 6. 예상 편집 순서

1. `AGENTS.md`의 기존 5개 섹션을 유지하되 제목과 내용을 재배치한다.
2. `Source Documents & Priority`, `REST API Work Rules`, `Security & Approval Boundaries` 섹션을 새로 추가한다.
3. 기존 `Build/Test` 섹션에 실제 MySQL 및 단일 테스트 명령을 보강한다.
4. 기존 `Coding Style` 섹션을 백엔드 공통 규칙 중심으로 확장한다.
5. 기존 `Commit & Pull Request` 섹션을 커밋 언어 규칙, 브랜치 전략, 금지 액션까지 확장한다.
6. 수정 후 `git diff -- AGENTS.md`로 범위가 `AGENTS.md`에만 한정됐는지 확인한다.

## 7. 검증 계획

- 문서 수정만 수행하므로 빌드/테스트는 실행하지 않는다.
- `Get-Content -Encoding UTF8 AGENTS.md`로 한글 인코딩이 깨지지 않는지 확인한다.
- `git diff -- AGENTS.md`로 의도한 문서 변경만 있는지 확인한다.
- `git status --short --ignored AGENTS.md`로 `AGENTS.md`가 ignored 상태인지 확인한다. 이 파일은 각자 스타일에 맞춰 구축하는 로컬 문서이므로 Git 추적 대상으로 만들지 않는다.

## 8. 확정 사항

- `AGENTS.md`는 “Codex가 실제 작업할 때 따라야 하는 로컬 운영 규칙 문서”로 확장한다. 따라서 concise contributor guide 형식에 맞추기 위해 필요한 작업 규칙을 줄이지 않는다.
- `AGENTS.md`는 Git에 올리지 않는다. 각 개발자/에이전트가 자신의 작업 스타일에 맞춰 로컬에서 관리한다.
- `CLAUDE.md`는 Claude용, `AGENTS.md`는 Codex용 문서로 본다. Codex가 Claude 문서를 계속 읽는다고 가정하지 않고, Claude 문서의 유효한 프로젝트 규칙 중 Codex에도 필요한 내용은 반드시 `AGENTS.md`에 요약 반영한다.
- 중복 규칙이 생기더라도 문제로 보지 않는다. 각 도구가 자기 문서만 읽어도 안전하게 작업할 수 있는 것이 우선이다.


