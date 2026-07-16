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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CollectionRepository collectionRepository;

    @BeforeEach
    void setUp() {
        insertCollection("Spring Series", "SERIES", "ACTIVE");
        insertCollection("Summer Event", "EVENT", "ACTIVE");
        insertCollection("Ended Event", "EVENT", "HIDDEN");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByStatusReturnsOnlyActiveCollections() {
        Page<Collection> page = collectionRepository.findByStatus("ACTIVE", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Collection::getName)
                .containsExactlyInAnyOrder("Spring Series", "Summer Event");
    }

    @Test
    void findByStatusAndCollectionTypeFiltersByType() {
        Page<Collection> page = collectionRepository.findByStatusAndCollectionType("ACTIVE", "EVENT", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Collection::getName).containsExactly("Summer Event");
    }

    // 관리자 컬렉션 API 구현계획(2026-07-16) §9.3 — 공개 컬렉션 API 회귀 확인.
    // AdminCollectionService.patchCollection()이 실제로 호출하는 것과 같은 도메인 메서드
    // (Collection.changeStatus)로 상태를 바꿨을 때, 공개 조회(findByStatus)가 여전히 ACTIVE만
    // 반환하는지 확인한다.
    @Test
    void 관리자가_ACTIVE를_HIDDEN으로_바꾸면_공개_findByStatus에서_제외된다() {
        Collection springSeries = collectionRepository.findByStatus("ACTIVE", PageRequest.of(0, 10))
                .getContent().stream()
                .filter(c -> "Spring Series".equals(c.getName()))
                .findFirst()
                .orElseThrow();

        springSeries.changeStatus("HIDDEN");
        entityManager.flush();
        entityManager.clear();

        Page<Collection> afterChange = collectionRepository.findByStatus("ACTIVE", PageRequest.of(0, 10));
        assertThat(afterChange.getContent()).extracting(Collection::getName)
                .containsExactly("Summer Event");
    }

    private void insertCollection(String name, String collectionType, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, collectionType)
                .setParameter(3, status)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
    }
}
