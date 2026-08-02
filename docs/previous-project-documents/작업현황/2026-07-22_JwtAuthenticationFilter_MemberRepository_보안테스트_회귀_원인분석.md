# 2026-07-22 JwtAuthenticationFilter MemberRepository 추가로 인한 보안 슬라이스 테스트 회귀 — 원인분석

## 0. 이 문서의 성격

- PR #64 코드리뷰 대응 작업(`2026-07-22_PR64_관리자-대여-결제-신고-회원탈퇴API_코드리뷰_대응계획.md`) **범위 밖**에서 발견된 별도 회귀 버그의 원인 기록이다.
- **2026-07-22 후속 세션에서 사용자 승인 후 §4(6개 SecurityIntegrationTest), §7.3(CollectionRepositoryTest)을 실제로 구현·검증 완료했다.** §8(`AdminBookRequestServiceConcurrencyIntegrationTest`)도 사용자가 직접 `V002__request_notification_notice_alignment.sql`을 로컬 DB에 적용해 해결 완료했다.
- **이 문서에 기록된 문제는 전부 해결됐다.** `./gradlew.bat build` 전체가 748 tests, 0 failed로 통과한다(2026-07-22 최종 확인).

## 1. 발견 경위

- PR64 대응 구현 완료 후 사용자가 로컬 DB를 최신화했고, `./gradlew.bat build` 전체 재실행을 요청함.
- DB 스키마 drift(`library.dtl_region_code` 컬럼 누락)로 인한 기존 실패 158건은 DB 동기화로 전부 해소됨(748 tests, 26 failed로 감소).
- 남은 26건 중 약 23건이 PR64와 무관한 `book`/`collection`/`library`/`request`/`notice` 도메인의 `*SecurityIntegrationTest`에서 발생. 이 테스트들은 그동안 DB 스키마 drift로 전체 빌드가 먼저 실패해 "실제로 통과/실패 판정까지 간 적이 없었기" 때문에, 이번에 DB를 고치고 나서야 처음 드러났다.

## 2. 원인

- 이전 세션(오늘, PR64 이전 라운드 — "관리자 대여·결제·신고·회원탈퇴 API" 최초 구현)에서 회원 탈퇴/제재 회원의 기존 JWT를 차단하기 위해 `JwtAuthenticationFilter`와 `SecurityConfig`에 `MemberRepository` 의존성을 추가했다.
  - `JwtAuthenticationFilter.isActiveMember(memberId)`가 `memberRepository.findById(memberId)`로 회원 상태(`JOINED`)를 확인한 뒤에만 `SecurityContext`에 인증 정보를 설정한다.
  - `SecurityConfig`가 `new JwtAuthenticationFilter(jwtTokenProvider, memberRepository)`로 필터를 생성하므로, `SecurityConfig` 빈을 만들려면 `MemberRepository` 빈이 반드시 있어야 한다.
- 그런데 아래 6개 테스트는 `@WebMvcTest`(웹 계층만 로드하는 슬라이스 테스트)를 쓰면서 `@Import(SecurityConfig.class)`로 실제 시큐리티 필터 체인을 함께 태운다. `@WebMvcTest`는 `@Repository`(JPA repository) 빈을 자동으로 스캔하지 않으므로, `MemberRepository` 빈이 컨텍스트에 없어 `SecurityConfig` 생성 단계에서 `NoSuchBeanDefinitionException`이 발생하고 컨텍스트 자체가 뜨지 않는다.

```
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name
'com.acorn.abc.common.config.SecurityConfig': Unsatisfied dependency expressed through constructor parameter 3:
No qualifying bean of type 'com.acorn.abc.member.repository.MemberRepository' available
```

- 즉, 오늘 이 세션에서 만든 `JwtAuthenticationFilter`/`SecurityConfig` 변경이 원인이며, PR64 코드리뷰 대응과는 무관하다. PR64 계획 문서에는 `MemberRepository.findByIdForUpdate()` 관련 두 줄(탈퇴/신고 제재 잠금)만 있을 뿐 이 회귀와는 관련이 없다.

## 3. 영향받는 파일과 정확한 위치

전부 `@WebMvcTest` + `@Import(SecurityConfig.class)` 조합을 쓰는 테스트다.

| 파일 | 실패 테스트 메서드 | 비고 |
|---|---|---|
| `src/test/java/com/acorn/abc/auth/controller/AuthSecurityIntegrationTest.java` | 3개 전부 | 이 클래스는 JWT를 아예 안 보내는 케이스만 있어, 빈만 추가하면 되고 상태 스텁은 불필요 |
| `src/test/java/com/acorn/abc/library/controller/LibraryControllerSecurityIntegrationTest.java` | 1개 전부 | 위와 동일(비회원 permitAll 케이스만 있음) |
| `src/test/java/com/acorn/abc/admin/collection/controller/AdminCollectionSecurityIntegrationTest.java` | `getCollections_USER_토큰이면_403을_반환한다`, `getCollections_ADMIN_토큰이면_200을_반환한다` | `jwtTokenProvider.generateToken(1L, "USER")`, `generateToken(99L, "ADMIN")`로 실제 토큰을 만들어 보냄 |
| `src/test/java/com/acorn/abc/admin/book/controller/AdminBookCrudSecurityIntegrationTest.java` | `getBooks_USER_토큰이면_403을_반환한다`, `getBooks_ADMIN_토큰이면_200을_반환한다` | 위와 동일 패턴(memberId 1L/99L) |
| `src/test/java/com/acorn/abc/admin/book/controller/AdminBookSecurityIntegrationTest.java` | `enrich_USER_토큰이면_403...`, `bulkApprove_ADMIN_토큰이면_200...`, `bulkDelete_USER_토큰이면_403...`, `bulkDelete_ADMIN_토큰이면_200...` | memberId 1L(USER) / 99L(ADMIN) 둘 다 사용. `enrich_미인증...`, `enrich_유효하지_않은_토큰...`은 토큰이 없거나 애초에 무효라 영향 없음 |
| `src/test/java/com/acorn/abc/request/controller/RequestNotificationNoticeSecurityIntegrationTest.java` | `admin_API는_USER_JWT에_403을_반환한다`, `admin_JWT는_관리자_API에_접근할_수_있다` | memberId 1L(USER) / 99L(ADMIN). `notices_...`, `bookRequest_미인증...`, `me_...401...`은 토큰이 없어 영향 없음 |

## 4. 해결 방법(시도했다가 되돌린 방향, 재착수 시 참고)

핵심: 위 6개 파일 각각에 `MemberRepository`를 목(mock) 빈으로 채워 컨텍스트가 뜨게 하고, **실제 유효한 JWT를 보내 인증 성공을 기대하는 테스트에서는** `memberRepository.findById(해당 memberId)`가 `status="JOINED"`인 회원을 반환하도록 스텁해야 한다. 스텁하지 않으면 `Optional.empty()`가 기본값이 되어 `isActiveMember()`가 항상 `false`가 되고, ADMIN 토큰을 보내도 401로 응답해 테스트가 다른 이유로 다시 깨진다.

1. import 추가: `com.acorn.abc.member.domain.Member`(스텁 대상 필요한 파일만), `com.acorn.abc.member.repository.MemberRepository`, `java.util.Optional`, `org.mockito.Mockito.mock`(필요한 파일만).
2. 클래스에 필드 추가:
   ```java
   @MockitoBean
   private MemberRepository memberRepository;
   ```
3. 실제 토큰(`jwtTokenProvider.generateToken(id, role)`)을 보내는 테스트에만, 요청 직전에 스텁 추가:
   ```java
   given(memberRepository.findById(1L)).willReturn(Optional.of(joinedMember()));
   // 또는 99L 등 해당 테스트가 쓰는 memberId
   ```
   `joinedMember()`는 `Member` mock에 `getStatus()`만 `"JOINED"`로 스텁하면 충분하다(역할은 필터가 JWT 클레임에서 읽으므로 `Member` mock의 role은 영향 없음).
4. `AuthSecurityIntegrationTest`, `LibraryControllerSecurityIntegrationTest`는 토큰 자체를 안 보내므로 2번(빈 추가)만 하면 되고 3번은 불필요.
5. `AdminBookSecurityIntegrationTest`의 `enrich_미인증...`, `enrich_유효하지_않은_토큰...`도 3번 불필요(토큰이 없거나 `validateToken()` 단계에서 걸러짐).

## 5. 왜 지금 안 고쳤는가 / 범위 판단

- 사용자가 "이거 계획에 있던 거냐, 아니냐"를 확인 요청했고, PR64 계획 문서에는 이 회귀에 대한 언급이 전혀 없음을 확인했다(§2 근거).
- 임의로 다른 도메인(book/collection/library/request/notice) 테스트 파일을 수정하는 것은 "현재 작업 범위와 무관한 파일은 손대지 않는다" 원칙에 위배되어, 사용자 요청에 따라 시도했던 수정을 전부 되돌렸다(`git restore`로 5개 파일 복구, PR64 커밋(`94f3d2b`)에는 영향 없음).
- 이 회귀는 오늘 이전 라운드 구현(JWT 상태 검증 추가)의 부작용이므로, PR64와는 별도 이슈/커밋으로 다루는 것이 맞다고 판단한다.

## 6. 다음 세션 시작 시 확인할 것

- 이 문서 §4의 방법대로 6개 파일을 수정할지 확정.
- 수정 시 `./gradlew.bat test --tests "com.acorn.abc.auth.controller.AuthSecurityIntegrationTest" --tests "com.acorn.abc.library.controller.LibraryControllerSecurityIntegrationTest" --tests "com.acorn.abc.admin.collection.controller.AdminCollectionSecurityIntegrationTest" --tests "com.acorn.abc.admin.book.controller.AdminBookCrudSecurityIntegrationTest" --tests "com.acorn.abc.admin.book.controller.AdminBookSecurityIntegrationTest" --tests "com.acorn.abc.request.controller.RequestNotificationNoticeSecurityIntegrationTest"`로 26건이 전부 해소되는지 확인.
- 남은 3건(`CollectionRepositoryTest` 2~3건, `AdminBookRequestServiceConcurrencyIntegrationTest` 1건)은 원인 종류가 다르다. `CollectionRepositoryTest` 3건은 §7에서 원인·수정 방향까지 정리했다. `AdminBookRequestServiceConcurrencyIntegrationTest` 1건은 아직 원인 미상(§8).

## 7. `CollectionRepositoryTest` — "내 로컬에서만 되는 테스트" 문제 (사용자 지적, 2026-07-22)

### 7.0 사용자가 지적한 규칙 (반드시 지킬 것)

- **"실 DB에 이 값이 들어있냐/없냐"를 판정 기준으로 삼는 단위 테스트는 절대 만들지 않는다.** 제약조건(UNIQUE/FK 등) 위반 검증이 아니라, "지금 테이블에 실제로 그 row가 있는지 조회해서 있으면 통과/없으면 실패" 식의 테스트를 말한다.
- 이유: 이 프로젝트는 각 개발자가 **각자 로컬 MySQL**을 쓴다(팀 공유 DB를 쓰는 경우도 있지만 기본은 로컬). 한 사람 로컬 DB에 우연히 맞는 데이터가 있어서 통과하는 테스트는 다른 사람 로컬에서는 실패한다 — "나만 통과하는 테스트"는 CI/협업 관점에서 의미가 없다.
- 증상으로 알아보는 법: 테스트 도중 MySQL에 직접 들어가서 관련 테이블 row를 지우면(또는 다른 데이터가 더 있으면) 테스트 결과가 바뀐다 — 이게 바로 "실 DB 상태에 결과가 좌우되는" 테스트라는 신호다.

### 7.1 `CollectionRepositoryTest`가 이 규칙을 위반하는 지점

파일: `src/test/java/com/acorn/abc/collection/repository/CollectionRepositoryTest.java`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // ← 임베디드 DB로 교체하지 않고 실제 설정된 MySQL을 그대로 쓴다
class CollectionRepositoryTest {
    @BeforeEach
    void setUp() {
        insertCollection("Spring Series", "SERIES", "ACTIVE");
        insertCollection("Summer Event", "EVENT", "ACTIVE");
        insertCollection("Ended Event", "EVENT", "HIDDEN");
        ...
    }

    @Test
    void findByStatusReturnsOnlyActiveCollections() {
        Page<Collection> page = collectionRepository.findByStatus("ACTIVE", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Collection::getName)
                .containsExactlyInAnyOrder("Spring Series", "Summer Event");  // ← 테이블 전체가 이 2건뿐이라고 가정
    }
```

- `findByStatus("ACTIVE")`는 `collection` 테이블 **전체**를 조회하는 쿼리다. `@BeforeEach`가 넣은 2건 외에 테이블에 이미 들어있는 다른 `ACTIVE` row가 있으면 그것까지 결과에 섞인다.
- 실제로 로컬 DB에는 이전 수동/API 연동 테스트로 만들어진 것으로 보이는 `ACTIVE` row가 이미 들어있었다: `"API연결테스트 시리즈 컬렉션"`, `"API연결테스트 이벤트 컬렉션"`, `"asdf"`. 그래서 `containsExactlyInAnyOrder("Spring Series", "Summer Event")`(정확히 이 2건만 있어야 통과)가 깨졌다.
- `@DataJpaTest`는 기본적으로 테스트 메서드를 트랜잭션으로 감싸고 끝나면 롤백하므로, **이 테스트 자신이 넣은 3건은 자동으로 정리된다.** 문제는 그게 아니라, 테스트 시작 시점에 **테이블에 이미 있던 다른 사람/다른 세션의 데이터까지 같이 조회돼서 assertion이 깨지는 것**이다.
- 이건 정확히 §7.0의 "실 DB에 뭐가 들어있냐"에 결과가 좌우되는 패턴이다. 각자 로컬 DB에 어떤 잔여 데이터가 있느냐에 따라 이 테스트는 통과할 수도, 실패할 수도 있다 — 즉 지금 이 테스트는 "나만 되는 테스트"일 가능성이 높다.
- 실패한 3건 전부 같은 원인이다(`findByStatusReturnsOnlyActiveCollections`, `findByStatusAndCollectionTypeFiltersByType`, `관리자가_ACTIVE를_HIDDEN으로_바꾸면_공개_findByStatus에서_제외된다`) — 셋 다 `findByStatus(...)`/`findByStatusAndCollectionType(...)` 결과를 `containsExactly`/`containsExactlyInAnyOrder`로 "정확히 이 값들만"이라고 단정한다.

### 7.2 여기서는 "DB 데이터를 지우는 것"이 답이 아닌 이유

- 로컬 DB에서 오염 row를 수동으로 지우면 **내 컴퓨터에서만** 일시적으로 통과한다. 다른 개발자 로컬에도 각자 그 사람 나름의 잔여 데이터가 있을 수 있으므로, 근본 해결이 아니라 §7.0에서 지적한 문제를 그대로 재현하는 임시방편이다.
- 게다가 이 프로젝트 규칙상 실 DB row를 지우는 건 "사전 확인 필요" 대상이라 매번 사용자 확인 없이 할 수도 없다.
- 따라서 고쳐야 할 대상은 **DB 데이터가 아니라 테스트 코드**다: 테스트가 "테이블 전체가 내가 넣은 데이터뿐"이라고 가정하지 않도록 만들어야 한다.

### 7.3 수정 방향 (다음 세션에서 실제 적용 시 참고 — 아직 미적용)

핵심 아이디어: **"내가 방금 넣은 row가 결과 안에 있는지/없는지"만 확인하고, "결과가 정확히 몇 건인지·그것뿐인지"는 확인하지 않는다.** 이렇게 하면 테이블에 다른 데이터가 몇 건 더 있어도(다른 개발자 로컬이든, 이전 세션 잔여 데이터든) 깨지지 않는다.

구체적으로:

1. `insertCollection(...)` 헬퍼가 생성된 `collectionId`(PK)를 반환하도록 바꾼다(현재는 반환값이 없다 — `entityManager.getEntityManager().createNativeQuery(...)`가 insert만 하고 ID를 안 돌려줌. `TestEntityManager.persistAndGetId(...)` 방식으로 바꾸거나, native insert 후 `SELECT LAST_INSERT_ID()`로 받아오면 된다).
2. 각 테스트에서 자기가 만든 row의 ID를 저장해두고, 조회 결과를 이름이 아니라 **ID 포함 여부**로 검증한다.
   ```java
   Long springSeriesId = insertCollection("Spring Series", "SERIES", "ACTIVE");
   Long summerEventId = insertCollection("Summer Event", "EVENT", "ACTIVE");
   Long endedEventId = insertCollection("Ended Event", "EVENT", "HIDDEN");
   ...
   Page<Collection> page = collectionRepository.findByStatus("ACTIVE", PageRequest.of(0, 10));
   List<Long> ids = page.getContent().stream().map(Collection::getCollectionId).toList();

   assertThat(ids).contains(springSeriesId, summerEventId);   // 내가 넣은 ACTIVE 2건은 반드시 포함
   assertThat(ids).doesNotContain(endedEventId);               // 내가 넣은 HIDDEN 1건은 반드시 제외
   // "정확히 이 N건뿐" 같은 전체 개수/전체 목록 단정은 하지 않는다 — 그건 실 DB 잔여 데이터에 좌우된다.
   ```
3. `findByStatusAndCollectionTypeFiltersByType()`, `관리자가_ACTIVE를_HIDDEN으로_바꾸면_공개_findByStatus에서_제외된다()`도 같은 패턴(ID 포함/제외 확인)으로 바꾼다.
4. `Collection` 도메인에 `getCollectionId()`(또는 동등한 접근자)가 이미 있는지 확인 후 없으면 `@Getter` 범위 안에서 노출 여부를 확인한다(`src/main/java/com/acorn/abc/collection/domain/Collection.java` — PK 컬럼명은 `collection_id`).
5. 페이지네이션(`PageRequest.of(0, 10)`) 크기가 실 데이터 오염으로 10건을 넘으면 내가 넣은 row가 결과에서 밀려날 수 있다 — 필요하면 `Pageable`을 늘리거나, `findAll`류로 바꿔서 페이지 크기 문제까지 배제한다.

### 7.4 다른 곳에도 같은 패턴이 있는지 함께 확인할 것

- 이번에 발견된 건 `CollectionRepositoryTest` 하나지만, `@AutoConfigureTestDatabase(replace = Replace.NONE)`을 쓰면서 `containsExactly`/`containsExactlyInAnyOrder`로 "테이블 전체 결과"를 단정하는 다른 `*RepositoryTest`가 더 있는지 다음 세션에서 훑어봐야 한다(예: `rg -n "containsExactly" src/test/java | rg -i repositorytest`).
- `AdminBookRequestServiceConcurrencyIntegrationTest`류처럼 `PREFIX`/`LIKE` 패턴 + `@AfterEach` 정리로 "내가 만든 데이터만" 스코프를 좁히는 기존 관례가 이미 프로젝트에 있다(§8 파일 상단 주석 참고) — `CollectionRepositoryTest`도 이름에 접두어를 붙이고 `LIKE` 필터를 쓰는 방식으로 통일해도 된다. ID 포함 확인 방식과 목적은 같다(내가 만든 데이터만 정확히 짚어서 검증).

## 8. `AdminBookRequestServiceConcurrencyIntegrationTest` — 원인 확인 완료 (로컬 DB 스키마 drift, 2026-07-22)

- 파일: `src/test/java/com/acorn/abc/admin/request/service/AdminBookRequestServiceConcurrencyIntegrationTest.java`
- 실패 테스트: `신규신청과_종료처리가_동시에_실행돼도_종료된_신청자에게만_알림이_생긴다()`
- 증상: `assertThat(newRequest.get(...).requestCandidateId()).isEqualTo(first.requestCandidateId())`(파일 126~127행)에서 `expected: 5L but was: 6L`. 같은 제목으로 거의 동시에 두 회원이 신청하면 같은 `book_request_candidate` 행으로 합쳐져야 하는데, 실제로는 서로 다른 candidate가 생성됐다.

### 확인된 원인

- `src/main/resources/schema.sql:754`는 `CREATE UNIQUE INDEX idx_book_request_candidate_normalized ON book_request_candidate (normalized_title, normalized_author, normalized_publisher);`로 **UNIQUE** 인덱스를 선언한다.
- 그런데 로컬 DB에서 `information_schema.STATISTICS`로 직접 확인한 결과, 실제로 적용된 `idx_book_request_candidate_normalized`는 `NON_UNIQUE=1`(일반 인덱스)이었다. 즉 UNIQUE 제약이 실제로는 걸려 있지 않다.
- `BookRequestCandidateRepository.upsertCandidate(...)`는 `INSERT ... ON DUPLICATE KEY UPDATE`로 이 UNIQUE 제약이 실제 DB에 걸려 있다는 전제 하에 "같은 정규화 3컬럼이면 새 행을 만들지 않고 기존 행만 갱신"하도록 짜여 있다. 로컬 DB에 UNIQUE 제약이 없으니 MySQL이 "중복"으로 판단하지 못해 매번 새 행을 INSERT하고, `LAST_INSERT_ID()`도 방금 새로 만든 행(6)을 가리켜 테스트가 기대한 기존 행(5)과 달라진다.
- `library.dtl_region_code` 컬럼 누락과 정확히 같은 종류의 문제다: `schema.sql`에 `CREATE UNIQUE INDEX`처럼 `IF NOT EXISTS`가 없는 DDL 구문은, 로컬 DB에 이름이 같은(하지만 속성이 다른) 인덱스가 이미 있으면 재실행 시 에러가 나고, 이 프로젝트의 `spring.sql.init.mode=always`+에러 무시 설정 때문에 그 에러가 조용히 무시돼 실제로는 반영되지 않는다.
- 코드(`BookRequestCandidateRepository`, `BookRequestService`, `AdminBookRequestService`)에는 버그가 없다. 실 데이터 존재 여부 판정 테스트도 아니다 — 이건 로컬 DB의 인덱스 속성이 `schema.sql`과 다른 순수 스키마 drift다.

### 확인 방법 (재현 시 참고)

- 임시 진단 테스트(`@SpringBootTest` + `JdbcTemplate`으로 `information_schema.STATISTICS` 조회)로 확인했고, 확인 후 즉시 삭제했다(코드에 남아있지 않음).
- 재확인하려면: `SELECT INDEX_NAME, NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book_request_candidate' AND INDEX_NAME = 'idx_book_request_candidate_normalized';` → `NON_UNIQUE`가 `1`이면 이 문제, `0`이면 정상.

### 해결 방법 및 처리 결과 (완료, 2026-07-22)

- `docs/데이터베이스/README.md` §6이 이미 이 문제를 다루는 기존 마이그레이션 파일을 문서화하고 있었다: `src/main/resources/db/manual/V002__request_notification_notice_alignment.sql`("희망도서/알림/공지 정합화가 안 된 기존 DB만" 대상).
- 이 파일의 1~4단계는 `book_request_candidate`의 정규화 3컬럼 중복 행을 대표 행으로 안전하게 병합하고, **5단계가 `idx_book_request_candidate_normalized`를 DROP 후 UNIQUE로 재생성**한다 — 즉 애드혹 DDL이 아니라 이미 검증된 절차가 있었다.
- 사용자가 직접 이 V002 파일을 로컬 DB에 적용했다. 적용 후 `AdminBookRequestServiceConcurrencyIntegrationTest`를 재실행해 `BUILD SUCCESSFUL`을 확인했고, 이어서 전체 `./gradlew.bat build`도 `BUILD SUCCESSFUL`(748 tests, 0 failed)로 확인했다.
- **§8은 해결 완료.** 코드 변경은 없었고(로컬 DB 스키마 drift였을 뿐), V002 마이그레이션 적용만으로 해소됐다.
