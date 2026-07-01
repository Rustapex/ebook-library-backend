package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
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
