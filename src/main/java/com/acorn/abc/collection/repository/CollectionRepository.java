package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.Collection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Page<Collection> findByStatus(String status, Pageable pageable);

    Page<Collection> findByStatusAndCollectionType(String status, String collectionType, Pageable pageable);

    // 관리자 목록 조회(API-ADMIN-COLLECTION-001)는 공개 API와 달리 ACTIVE로 강제하지 않고
    // type/status 둘 다 선택적으로 필터링한다(구현계획 §5.4).
    @Query("SELECT c FROM Collection c " +
            "WHERE (:collectionType IS NULL OR c.collectionType = :collectionType) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Collection> searchForAdmin(@Param("collectionType") String collectionType,
                                     @Param("status") String status,
                                     Pageable pageable);

    // API-003/004의 write 경로 전용 — 같은 컬렉션에 대한 동시 요청을 직렬화한다(D-12).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Collection c WHERE c.collectionId = :collectionId")
    Optional<Collection> findByIdForUpdate(@Param("collectionId") Long collectionId);
}
