package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.ReadingProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    Optional<ReadingProgress> findByRental_RentalId(Long rentalId);

    long countByCompletedYn(Boolean completedYn);

    @Query("SELECT rp FROM ReadingProgress rp JOIN FETCH rp.rental r JOIN FETCH r.book " +
            "WHERE r.member.memberId = :memberId AND rp.lastReadAt IS NOT NULL " +
            "ORDER BY rp.lastReadAt DESC")
    List<ReadingProgress> findRecentByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
