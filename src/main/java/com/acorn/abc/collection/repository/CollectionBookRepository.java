package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.CollectionBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionBookRepository extends JpaRepository<CollectionBook, Long> {

    // API-ADMIN-COLLECTION-006 상세 조회, SERIES 정렬(구현계획 §6.3).
    List<CollectionBook> findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(Long collectionId);

    // API-ADMIN-COLLECTION-006 상세 조회, EVENT 정렬(D-07 — EVENT는 displayOrder를 쓰지 않고 createdAt 순).
    List<CollectionBook> findByCollection_CollectionIdOrderByCreatedAtAscCollectionBookIdAsc(Long collectionId);

    // API-003 upsert 시 요청 bookId 중 기존 mapping을 찾기 위한 조회.
    List<CollectionBook> findByCollection_CollectionIdAndBook_BookIdIn(Long collectionId, List<Long> bookIds);

    // D-08 — mapping이 하나라도 있으면 collectionType 변경을 거부한다.
    boolean existsByCollection_CollectionId(Long collectionId);

    // API-001/006 — 컬렉션 목록/상세 응답의 mappedCount.
    long countByCollection_CollectionId(Long collectionId);

    // API-001 목록 — 컬렉션 ID 목록별 mapped count를 한 번에 조회해 N+1을 방지한다(구현계획 §5.4).
    @Query("SELECT cb.collection.collectionId AS collectionId, COUNT(cb) AS mappedCount " +
            "FROM CollectionBook cb " +
            "WHERE cb.collection.collectionId IN :collectionIds " +
            "GROUP BY cb.collection.collectionId")
    List<CollectionMappedCountView> countByCollectionIds(@Param("collectionIds") List<Long> collectionIds);

    // API-004 — 지정 bookId mapping만 삭제하고 삭제 건수를 반환한다(idempotent — 이미 없는 bookId는 0건 삭제).
    long deleteByCollection_CollectionIdAndBook_BookIdIn(Long collectionId, List<Long> bookIds);

    interface CollectionMappedCountView {
        Long getCollectionId();

        Long getMappedCount();
    }
}
