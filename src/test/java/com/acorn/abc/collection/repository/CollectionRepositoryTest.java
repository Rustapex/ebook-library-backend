package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 2026-07-22 JwtAuthenticationFilter MemberRepository 보안테스트 회귀 원인분석 §7 대응 —
// 이 테스트는 각자 로컬 MySQL을 그대로 쓰는 @AutoConfigureTestDatabase(Replace.NONE)이라, collection
// 테이블에 이미 있는 다른 데이터(다른 개발자 로컬 잔여 데이터 등)와 무관해야 한다. "테이블 전체가 내가
// 넣은 데이터뿐"이라고 단정(containsExactly/containsExactlyInAnyOrder)하지 않고, 내가 방금 넣은 row의
// ID가 결과에 포함/제외되는지만 확인한다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectionRepositoryTest {

    // 실 데이터가 이 값보다 많이 쌓여 있어도(다른 개발자 로컬 잔여 데이터) 내가 넣은 row가
    // 페이지 밖으로 밀려나지 않도록 넉넉하게 잡는다.
    private static final PageRequest LARGE_PAGE = PageRequest.of(0, 1000);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CollectionRepository collectionRepository;

    private Long springSeriesId;
    private Long summerEventId;
    private Long endedEventId;

    @BeforeEach
    void setUp() {
        springSeriesId = insertCollection("Spring Series", "SERIES", "ACTIVE");
        summerEventId = insertCollection("Summer Event", "EVENT", "ACTIVE");
        endedEventId = insertCollection("Ended Event", "EVENT", "HIDDEN");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByStatusReturnsOnlyActiveCollections() {
        Page<Collection> page = collectionRepository.findByStatus("ACTIVE", LARGE_PAGE);
        List<Long> ids = page.getContent().stream().map(Collection::getCollectionId).toList();

        assertThat(ids).contains(springSeriesId, summerEventId);
        assertThat(ids).doesNotContain(endedEventId);
    }

    @Test
    void findByStatusAndCollectionTypeFiltersByType() {
        Page<Collection> page = collectionRepository.findByStatusAndCollectionType("ACTIVE", "EVENT", LARGE_PAGE);
        List<Long> ids = page.getContent().stream().map(Collection::getCollectionId).toList();

        assertThat(ids).contains(summerEventId);
        assertThat(ids).doesNotContain(springSeriesId, endedEventId);
    }

    // 관리자 컬렉션 API 구현계획(2026-07-16) §9.3 — 공개 컬렉션 API 회귀 확인.
    // AdminCollectionService.patchCollection()이 실제로 호출하는 것과 같은 도메인 메서드
    // (Collection.changeStatus)로 상태를 바꿨을 때, 공개 조회(findByStatus)가 여전히 ACTIVE만
    // 반환하는지 확인한다.
    @Test
    void 관리자가_ACTIVE를_HIDDEN으로_바꾸면_공개_findByStatus에서_제외된다() {
        Collection springSeries = collectionRepository.findByStatus("ACTIVE", LARGE_PAGE)
                .getContent().stream()
                .filter(c -> springSeriesId.equals(c.getCollectionId()))
                .findFirst()
                .orElseThrow();

        springSeries.changeStatus("HIDDEN");
        entityManager.flush();
        entityManager.clear();

        Page<Collection> afterChange = collectionRepository.findByStatus("ACTIVE", LARGE_PAGE);
        List<Long> ids = afterChange.getContent().stream().map(Collection::getCollectionId).toList();

        assertThat(ids).doesNotContain(springSeriesId);
        assertThat(ids).contains(summerEventId);
    }

    // native insert 후 SELECT LAST_INSERT_ID()로 같은 트랜잭션의 커넥션에서 생성된 PK를 그대로 받아온다.
    private Long insertCollection(String name, String collectionType, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, collectionType)
                .setParameter(3, status)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();

        Object generatedId = entityManager.getEntityManager()
                .createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult();
        return ((Number) generatedId).longValue();
    }
}
