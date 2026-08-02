# `archTest`용 ArchUnit 의존성 추가 — 검토 문서

목적: `archTest`(ArchUnit 기반 아키텍처 규칙 검증)를 도입하려면 `build.gradle`을 수정해야 하는데, 이 브랜치는 "1-0 원복 이후 `build.gradle` 추가 수정 금지" 원칙(`2026-07-02_카탈로그API_수정계획.md` 0절)을 쓰고 있어 예외 승인이 필요한 상태다. 팀장에게 요청하거나 직접 구현할 때 쓸 근거 자료로 무엇을 왜 고치는지, 고치면 뭐가 바뀌는지 정리한다.

관련 계획 항목: `2026-07-02_카탈로그API_수정계획.md` 2-4절, 0절

---

## 1. 왜 필요한가 (근거)

- `CLAUDE.md`(프로젝트 루트) "작업 완료 조건": *"백엔드 코드를 수정했으면 `.\gradlew.bat archTest` 로 규칙 위반 여부를 확인하고, 실패 항목이 없어야 작업을 완료로 본다."*
- `.claude/rules/backend-convention.md` 마지막 줄에도 동일한 요구 반복.
- 단, `docs/공통문서/백엔드_공통_개발_규칙.md` 자체(14절 "초기 구현 우선순위" 등)에는 ArchUnit/`archTest`를 명시적으로 요구하는 조항이 없다. **즉 `archTest` 요구는 팀 공통 규칙 문서가 아니라, 이 프로젝트의 로컬 작업 지침(`CLAUDE.md`)에서 온 것이다.** 팀장에게 요청할 때 "규칙.md에 있는 요구사항"이 아니라 "우리 쪽 작업 완료 조건으로 설정해둔 게이트"라는 점을 구분해서 전달할 필요가 있음.
- 확인 결과(2026-07-02) `build.gradle`에 ArchUnit 의존성 및 `archTest` task가 **아직 존재하지 않는다.** 즉 지금은 `.\gradlew.bat archTest` 자체가 실행 불가능한 상태 — 완료 조건이 있는데 그 조건을 검증할 수단이 없는 상태다.

## 2. 수정해야 하는 파일과 내용

### 2-1. `build.gradle` (수정)

현재 상태(확인됨):
- `dependencies { ... }` 블록: 21~38행, ArchUnit 관련 항목 없음.
- `tasks.named('test') { useJUnitPlatform() }`: 40~42행, `archTest`라는 이름의 task는 없음.

추가할 내용 2가지:

1. **의존성 1줄** — `testImplementation` 스코프에 ArchUnit-JUnit5 모듈 추가.
   ```gradle
   testImplementation 'com.tngtech.archunit:archunit-junit5:<최신 안정 버전>'
   ```
   - 정확한 버전은 Maven Central(`https://mvnrepository.com/artifact/com.tngtech.archunit/archunit-junit5`)에서 최신 안정 버전을 직접 확인해서 명시할 것(이 문서에서 특정 버전을 단정하지 않음).
   - `testImplementation`이므로 프로덕션 런타임(jar)에는 포함되지 않음.

2. **`archTest` task 신규 정의** — 기존 `test` task와 분리해서 아키텍처 테스트만 따로 실행.
   ```gradle
   tasks.register('archTest', Test) {
       useJUnitPlatform()
       filter {
           includeTestsMatching "*ArchitectureTest"
       }
   }
   ```
   - `filter.includeTestsMatching`으로 클래스명이 `*ArchitectureTest`인 테스트만 골라 실행 — 별도 소스셋을 만들지 않고 기존 `src/test/java` 안에 두는 방식(계획 문서 2-4에서 예정한 `src/test/java/.../ArchitectureTest.java` 경로와 일치).
   - `tasks.named('test')`는 그대로 두고 `archTest`를 새 task로 추가하는 것이라, 기존 `.\gradlew.bat test`/`build` 동작은 바뀌지 않는다(3절 참고).

### 2-2. `src/test/java/com/acorn/abc/ArchitectureTest.java` (신규, `build.gradle` 범위 밖)

- 규칙.md 2~10절(응답 포맷, DTO 네이밍, 패키지 구조, `BaseEntity` 상속 등)을 ArchUnit 규칙(`ArchRuleDefinition`, `ArchRule` 등)으로 어서션화하는 실제 테스트 클래스.
- 이건 `build.gradle` 수정과 무관한 별도 작업이라 본 문서(의존성 추가 분석) 범위 밖이며, 계획 문서 2-4절에서 다룸.

---

## 3. `build.gradle` 수정 시 영향 분석

| 항목 | 영향 |
|---|---|
| 프로덕션 런타임(jar) | 없음 — `testImplementation`이라 실행 아티팩트에 포함 안 됨 |
| 기존 `.\gradlew.bat test`/`build` | 없음 — `archTest`는 별도 신규 task, 기존 `test` task를 바꾸거나 대체하지 않음 |
| CI 게이트화 여부 | `archTest`를 `check`/`build`가 자동으로 의존하게(`build.dependsOn archTest` 등) 만들지는 이 변경에 포함하지 않음 — 별도 팀 결정 필요. 게이트화하면 이후 모든 PR이 이 테스트를 통과해야 하므로 영향 범위가 커짐 |
| 팀원 환경 | Gradle 의존성 변경이라 각자 `.\gradlew.bat build` 또는 IDE에서 Gradle 재동기화 필요, Maven Central 접근(사내망/오프라인 환경이면 미러 확인) 필요 |
| JUnit 5 호환성 | 이미 `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`(36행) 존재 확인 — ArchUnit-JUnit5 모듈과 충돌 없음 |
| Gradle 버전 | `gradle-wrapper.properties` 기준 Gradle 9.5.1 — 최신 ArchUnit 버전과 호환 이슈 없음(2026-07-02 기준 확인) |
| 이번 브랜치 "파일 수정 범위 원칙" | 정면 충돌 — "1-0 원복 이후 `build.gradle` 추가 수정 금지" 원칙에 대한 **명시적 예외**로 처리해야 함. 즉 이 변경은 일반 코드 수정이 아니라 **원칙에 대한 예외 승인 + 리뷰가 필요한 변경**으로 다뤄야 함 |

## 4. 요청/구현 시 체크리스트

- [ ] ArchUnit-JUnit5 최신 버전 확인(Maven Central)
- [ ] `build.gradle`에 의존성 1줄 + `archTest` task 정의 추가
- [ ] `archTest`를 `build`/`check`에 자동 연결할지 여부 팀 결정(권장: 처음엔 연결 안 하고 수동 실행으로 시작 → 안정화 후 게이트화)
- [ ] "파일 수정 범위 원칙" 예외 승인권자(팀장) 확인 및 커밋/PR 본문에 예외 사유 기록
- [ ] 변경 후 `.\gradlew.bat archTest`(빈 규칙이라도) 정상 실행 확인, `.\gradlew.bat build` 기존 동작 회귀 없는지 확인
