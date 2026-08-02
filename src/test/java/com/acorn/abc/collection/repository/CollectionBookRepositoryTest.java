package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.CollectionBook;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 컬렉션 API 구현계획(2026-07-16) §9.3 — mapping·count·정렬·lock 관련 실DB 동작 검증.
 * write lock(PESSIMISTIC_WRITE) 자체의 동시 요청 직렬화는 멀티스레드 테스트 없이는 증명할 수 없어
 * 이 테스트는 findByIdForUpdate가 정상 조회되는지까지만 확인한다(수동 동시 요청 검증은 별도 Postman
 * 시나리오로 다룬다 — 이해메모/설계문서 반영예정에 한계로 기록).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectionBookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private CollectionBookRepository collectionBookRepository;

    private Long publisherId;
    private Long seriesCollectionId;
    private Long eventCollectionId;
    private Long bookAId;
    private Long bookBId;

    @BeforeEach
    void setUp() {
        publisherId = insertPublisher("Admin Collection Test Publisher");
        seriesCollectionId = insertCollection("Admin Series", "SERIES", "HIDDEN");
        eventCollectionId = insertCollection("Admin Event", "EVENT", "ENDED");
        bookAId = insertBook("Book A", "AVAILABLE");
        bookBId = insertBook("Book B", "HIDDEN");

        insertCollectionBook(seriesCollectionId, bookBId, 2);
        insertCollectionBook(seriesCollectionId, bookAId, 1);
        insertCollectionBook(eventCollectionId, bookAId, null);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchForAdmin_type과_status를_동시에_필터링한다() {
        Page<com.acorn.abc.collection.domain.Collection> page =
                collectionRepository.searchForAdmin("SERIES", "HIDDEN", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(com.acorn.abc.collection.domain.Collection::getCollectionId)
                .containsExactly(seriesCollectionId);
    }

    @Test
    void searchForAdmin_필터가_없으면_HIDDEN_ENDED도_전부_반환한다() {
        Page<com.acorn.abc.collection.domain.Collection> page =
                collectionRepository.searchForAdmin(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(com.acorn.abc.collection.domain.Collection::getCollectionId)
                .contains(seriesCollectionId, eventCollectionId);
    }

    @Test
    void findByIdForUpdate_존재하는_컬렉션을_조회한다() {
        Optional<com.acorn.abc.collection.domain.Collection> found =
                collectionRepository.findByIdForUpdate(seriesCollectionId);

        assertThat(found).isPresent();
        assertThat(found.get().getCollectionId()).isEqualTo(seriesCollectionId);
    }

    @Test
    void countByCollectionIds_컬렉션ID_목록별_mappedCount를_batch로_반환한다() {
        List<CollectionBookRepository.CollectionMappedCountView> counts =
                collectionBookRepository.countByCollectionIds(List.of(seriesCollectionId, eventCollectionId));

        assertThat(counts).extracting(CollectionBookRepository.CollectionMappedCountView::getCollectionId)
                .containsExactlyInAnyOrder(seriesCollectionId, eventCollectionId);
        assertThat(counts.stream()
                .filter(v -> v.getCollectionId().equals(seriesCollectionId))
                .findFirst().orElseThrow().getMappedCount()).isEqualTo(2L);
    }

    @Test
    void findByCollection_SERIES는_displayOrder_오름차순으로_정렬된다() {
        List<CollectionBook> books = collectionBookRepository
                .findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(seriesCollectionId);

        assertThat(books).extracting(cb -> cb.getBook().getBookId()).containsExactly(bookAId, bookBId);
    }

    @Test
    void findByCollection_EVENT는_displayOrder가_null이어도_createdAt_순으로_정렬된다() {
        List<CollectionBook> books = collectionBookRepository
                .findByCollection_CollectionIdOrderByCreatedAtAscCollectionBookIdAsc(eventCollectionId);

        assertThat(books).extracting(cb -> cb.getBook().getBookId()).containsExactly(bookAId);
        assertThat(books.get(0).getDisplayOrder()).isNull();
    }

    @Test
    void existsByCollection_CollectionId_mapping이_있으면_true를_반환한다() {
        assertThat(collectionBookRepository.existsByCollection_CollectionId(seriesCollectionId)).isTrue();
    }

    @Test
    void existsByCollection_CollectionId_mapping이_없으면_false를_반환한다() {
        Long emptyCollectionId = insertCollection("Empty Series", "SERIES", "ACTIVE");

        assertThat(collectionBookRepository.existsByCollection_CollectionId(emptyCollectionId)).isFalse();
    }

    @Test
    void deleteByCollection_CollectionIdAndBook_BookIdIn_지정_bookId만_삭제하고_삭제건수를_반환한다() {
        long deleted = collectionBookRepository.deleteByCollection_CollectionIdAndBook_BookIdIn(
                seriesCollectionId, List.of(bookBId));

        assertThat(deleted).isEqualTo(1L);
        assertThat(collectionBookRepository.countByCollection_CollectionId(seriesCollectionId)).isEqualTo(1L);
    }

    @Test
    void deleteByCollection_CollectionIdAndBook_BookIdIn_이미_없는_bookId는_0건_삭제로_idempotent하다() {
        long deleted = collectionBookRepository.deleteByCollection_CollectionIdAndBook_BookIdIn(
                seriesCollectionId, List.of(999_999L));

        assertThat(deleted).isEqualTo(0L);
    }

    private Long insertPublisher(String name) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO publisher (publisher_name, status, created_at) VALUES (?, 'ACTIVE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("publisher").longValue();
    }

    private Long insertBook(String title, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (publisher_id, title, rental_type, rental_price, " +
                        "default_rental_days, total_pages, rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, ?, 'FREE', 0, 7, 100, 0, 0.00, 0, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, publisherId)
                .setParameter(2, title)
                .setParameter(3, status)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book").longValue();
    }

    private Long insertCollection(String name, String collectionType, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, collectionType)
                .setParameter(3, status)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("collection").longValue();
    }

    private void insertCollectionBook(Long collectionId, Long bookId, Integer displayOrder) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, collectionId)
                .setParameter(2, bookId)
                .setParameter(3, displayOrder)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
