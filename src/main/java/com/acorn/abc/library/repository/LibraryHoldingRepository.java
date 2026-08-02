package com.acorn.abc.library.repository;

import com.acorn.abc.library.domain.LibraryHolding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibraryHoldingRepository extends JpaRepository<LibraryHolding, Long> {

    Optional<LibraryHolding> findByLibrary_LibraryIdAndIsbn(Long libraryId, String isbn);

    void deleteByLibrary_LibraryIdAndIsbn(Long libraryId, String isbn);

    List<LibraryHolding> findAllByLibrary_LibraryId(Long libraryId);

    // 관리자 수동 fallback 조회: 도서 ISBN + region(+ 선택적 dtlRegion) 기준으로
    // ACTIVE 도서관만 조회한다. 요청 page/size를 그대로 반영해 pagination metadata를 유지한다.
    @Query("SELECT lh FROM LibraryHolding lh JOIN FETCH lh.library l " +
            "WHERE lh.isbn = :isbn AND l.regionCode = :regionCode AND l.status = 'ACTIVE' " +
            "AND (:dtlRegionCode IS NULL OR l.dtlRegionCode = :dtlRegionCode)")
    Page<LibraryHolding> findManualFallback(@Param("isbn") String isbn,
                                             @Param("regionCode") String regionCode,
                                             @Param("dtlRegionCode") String dtlRegionCode,
                                             Pageable pageable);
}
