# `CategoryRepositoryTest` 실패 원인 및 수정 허가 요청

- 작성일: 2026-07-08
- 작성자: (PR #33 담당자, 챌린지/출석/포인트/쿠폰 도메인)
- 목적: `book`/`category` 도메인 담당자에게 원인과 수정 계획을 공유하고, **이 파일을 직접 수정해도 되는지 허락을 구하기 위한 문서**
- 상태: **원인 분석 + 수정 계획만, 코드 미수정.** 담당자 승인 전에는 손대지 않는다.

---

## 0. 왜 이 문서를 쓰는가

`CategoryRepositoryTest`는 `book`/`category` 도메인 소속 파일로, 내 작업 범위(챌린지/출석/포인트/쿠폰, PR #33)가 아니다. `.\gradlew.bat test`를 돌릴 때마다 이 테스트 1건만 계속 실패해서 전체 테스트 통과 여부를 매번 헷갈리게 만들고 있어, 원인을 분석하고 수정 방향을 정리했다. **직접 고칠 권한이 없다고 판단해 실제 수정은 하지 않았고, 담당자 승인을 받은 뒤에만 진행한다.**

---

## 1. 실패 위치

- 파일: `src/test/java/com/acorn/abc/category/repository/CategoryRepositoryTest.java`
- 테스트 메서드: `findsOnlyActiveCategoriesOrderedByDisplayOrder()` (36~41행)
- 다른 파일에서 이 클래스/메서드를 참조하는 곳 없음(`rg`로 확인 완료) — 수정 시 영향 범위가 이 파일 하나로 닫혀 있음.

---

## 2. 실패 원인 (코드 근거 기반)

전체 파일(53줄, 짧아서 전체 확인함):

```java
@BeforeEach
void setUp() {
    insertCategory(null, "Novel", 2, "ACTIVE");
    insertCategory(null, "Essay", 1, "ACTIVE");
    insertCategory(null, "Retired", 0, "INACTIVE");

    entityManager.flush();
    entityManager.clear();
}

@Test
void findsOnlyActiveCategoriesOrderedByDisplayOrder() {
    List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE");

    assertThat(categories).extracting(Category::getCategoryName).containsExactly("Essay", "Novel");
    assertThat(categories).extracting(Category::getStatus).allMatch("ACTIVE"::equals);
}
```

- 클래스에 `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`가 붙어 있음 — 임베디드 DB로 교체하지 않고 **실제 설정된(공유) DB**를 그대로 사용한다는 뜻.
- `@DataJpaTest`는 테스트마다 트랜잭션을 롤백하므로, 이 테스트가 직접 만든 `Novel`/`Essay`/`Retired` 3건은 테스트가 끝나면 사라진다. 즉 "이 테스트 자신이 남긴 잔여 데이터" 문제는 아니다.
- **진짜 원인**: `findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE")`는 이름/부모 조건 없이 **테이블 전체에서 `status='ACTIVE'`인 행을 전부** 가져온다. 그런데 검증부는 `containsExactly("Essay", "Novel")` — "결과가 정확히 이 2건뿐이어야 통과"라는 뜻이다. 공유 DB에 이 테스트가 만들지 않은 다른 `ACTIVE` 카테고리가 하나라도 있으면(실제 서비스용 카테고리 데이터, 다른 개발자가 수동으로 넣은 데이터 등) 무조건 실패한다.
- `src/main/resources/data.sql`을 확인한 결과 `category` 테이블에 대한 시드 데이터는 없음(`member_grade`만 시드) — 즉 이 실패가 프로젝트 공식 시드 데이터 때문은 아니고, **공유 개발 DB에 실제로 쌓인 카테고리 데이터** 때문일 가능성이 높다.
- **확인 못 한 부분(가정)**: 나는 실제 공유 DB에 접속할 권한/도구가 없어서, "정말 다른 ACTIVE 카테고리가 있는지"는 DB를 직접 조회해서 확인하지 못했다. 위 원인 분석은 테스트 코드와 쿼리 메서드 시그니처만으로 추론한 것이다. **담당자가 실제 DB에서 `SELECT * FROM category WHERE status='ACTIVE'`로 한 번 확인해주면 원인이 100% 확정된다.**

### 참고 — 같은 종류의 문제를 이미 겪은 사례 (PR #31)

PR #31(`test(book): BookApprovalServiceTest 실DB 잔여 데이터 flaky 실패 수정`)에서 비슷한 "실DB 격리" 문제를 다룬 적이 있다. 다만 PR #31의 원인은 `REQUIRES_NEW` 트랜잭션 때문에 롤백이 안 먹는 경우였고, 이번 건은 트랜잭션 롤백은 정상 동작하되 **애초에 조회 쿼리가 이 테스트의 fixture로 범위를 좁히지 않는다**는 점이 다르다. 원인은 다르지만 "실제/다른 데이터와 안 겹치게 테스트를 격리한다"는 철학은 동일하게 적용 가능하다.

---

## 3. 수정 계획 (승인 시 진행할 안)

### 안 A — 최소 수정 (권장)

`containsExactly` → `containsSubsequence`로 교체(AssertJ 제공 메서드, 1줄 변경):

```java
assertThat(categories).extracting(Category::getCategoryName).containsSubsequence("Essay", "Novel");
assertThat(categories).extracting(Category::getStatus).allMatch("ACTIVE"::equals);
```

- `containsExactly`: "리스트가 정확히 이 원소들로만, 이 순서대로 구성돼야 함" — 다른 ACTIVE 카테고리가 섞이면 즉시 실패.
- `containsSubsequence`: "주어진 원소들이 이 상대 순서대로 나타나기만 하면 됨, 그 사이/앞뒤에 다른 원소가 있어도 통과" — 이 테스트가 원래 검증하려던 것(display_order 오름차순 정렬이 맞는지, `Essay`가 `Novel`보다 먼저 나오는지)은 그대로 지키면서, 공유 DB의 다른 ACTIVE 카테고리 존재 여부에는 영향을 받지 않게 됨.
- 두 번째 assertion(`allMatch("ACTIVE"::equals)`)은 그대로 유지 — `INACTIVE`인 `Retired`가 결과에 안 섞이는지 확인하는 부분이라 손댈 필요 없음.
- **장점**: 변경이 1줄이라 리스크가 가장 작고, 테스트 의도(정렬 검증)를 그대로 유지함.
- **단점**: 여전히 실제 DB를 조회하는 통합 테스트라 DB 상태에 어느 정도 의존함(다만 이번 실패의 직접 원인만 제거).

### 안 B — 근본적 격리 (대안, 더 큰 변경)

PR #31 방식처럼 이 테스트가 만든 행의 ID만 추적해서, 조회 결과를 그 ID로 필터링한 뒤 검증:

```java
private Long novelId;
private Long essayId;

@BeforeEach
void setUp() {
    novelId = insertCategory(null, "Novel", 2, "ACTIVE");
    essayId = insertCategory(null, "Essay", 1, "ACTIVE");
    insertCategory(null, "Retired", 0, "INACTIVE");
    ...
}

@Test
void findsOnlyActiveCategoriesOrderedByDisplayOrder() {
    List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE")
            .stream()
            .filter(c -> c.getCategoryId().equals(novelId) || c.getCategoryId().equals(essayId))
            .toList();

    assertThat(categories).extracting(Category::getCategoryName).containsExactly("Essay", "Novel");
    ...
}
```

- `insertCategory()`가 생성된 ID를 반환하도록 바꿔야 함(다른 repository 테스트의 `lastInsertId()` 헬퍼 패턴 참고 가능).
- **장점**: `containsExactly`를 그대로 유지할 수 있어 원래 검증 강도가 더 강함.
- **단점**: 변경 범위가 안 A보다 크고(헬퍼 메서드 반환 타입 변경 포함), 이 테스트 파일만의 문제가 아니라 같은 패턴을 쓰는 다른 테스트(`CollectionRepositoryTest` 등)에도 비슷한 리스크가 있을 수 있어 일관성 논의가 필요함.

### 권장

**안 A를 권장한다.** 이번 실패의 직접 원인만 제거하는 최소 변경이고, 이 파일 담당자가 나중에 더 강한 격리가 필요하다고 판단하면 안 B로 확장하기도 쉽다.

---

## 4. 검증 방법 (승인 후 진행 시)

1. 안 A 적용 후 `.\gradlew.bat test --tests com.acorn.abc.category.repository.CategoryRepositoryTest` 단독 실행해서 통과 확인.
2. `.\gradlew.bat test` 전체 실행해서 다른 테스트에 영향 없는지 확인(이 파일을 참조하는 다른 코드가 없음을 이미 `rg`로 확인했으므로 회귀 위험은 낮음).
3. 가능하면 담당자가 실제 공유 DB에서 `SELECT category_name, status FROM category WHERE status='ACTIVE'`로 원인 가정(다른 ACTIVE 카테고리 존재)이 맞는지 한 번 확인.

---

## 5. 승인 요청

- **요청 사항**: 위 안 A(`containsExactly` → `containsSubsequence`, 1줄 변경)로 `CategoryRepositoryTest.java`를 내가 직접 수정해도 되는지 허락 부탁드립니다.
- **범위**: 이 파일 1개, 1줄만 변경. 다른 `book`/`category`/`collection` 관련 파일은 건드리지 않습니다.
- **원치 않으시면**: 원인 분석까지만 참고하시고, 실제 수정은 담당자분이 직접 진행하셔도 됩니다.
