package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.ReadingProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    Optional<ReadingProgress> findByRental_RentalId(Long rentalId);

    long countByCompletedYn(Boolean completedYn);

    // 관리자 통계 완독 기간/나이대 조건부 집계(관리자 통계 확장 §3.1~§3.3). 회원은 rental.member 경유.
    @Query("SELECT COUNT(rp) FROM ReadingProgress rp WHERE rp.completedYn = true " +
            "AND (:from IS NULL OR rp.completedAt >= :from) AND (:to IS NULL OR rp.completedAt < :to) " +
            "AND (:birthDateFrom IS NULL OR rp.rental.member.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR rp.rental.member.birthDate <= :birthDateTo)")
    long countCompletedByStatisticConditions(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              @Param("birthDateFrom") LocalDate birthDateFrom,
                                              @Param("birthDateTo") LocalDate birthDateTo);

    interface DailyCountBucket {
        LocalDate getBucketDate();
        Long getBucketCount();
    }

    interface MonthlyCountBucket {
        String getBucketMonth();
        Long getBucketCount();
    }

    // 관리자 통계 추이(completedReadTrend) — bucket 반복 조회 대신 기간 전체를 한 번에 GROUP BY로 집계한다(PR #42 리뷰 항목 2 반영).
    @Query(value = "SELECT DATE(rp.completed_at) AS bucketDate, COUNT(*) AS bucketCount " +
            "FROM reading_progress rp JOIN book_rental r ON rp.rental_id = r.rental_id JOIN member m ON r.member_id = m.member_id " +
            "WHERE rp.completed_yn = 1 " +
            "AND (:from IS NULL OR rp.completed_at >= :from) AND (:to IS NULL OR rp.completed_at < :to) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE(rp.completed_at)", nativeQuery = true)
    List<DailyCountBucket> groupByDayCompletedStatisticConditions(@Param("from") LocalDateTime from,
                                                                   @Param("to") LocalDateTime to,
                                                                   @Param("birthDateFrom") LocalDate birthDateFrom,
                                                                   @Param("birthDateTo") LocalDate birthDateTo);

    @Query(value = "SELECT DATE_FORMAT(rp.completed_at, '%Y-%m') AS bucketMonth, COUNT(*) AS bucketCount " +
            "FROM reading_progress rp JOIN book_rental r ON rp.rental_id = r.rental_id JOIN member m ON r.member_id = m.member_id " +
            "WHERE rp.completed_yn = 1 " +
            "AND (:from IS NULL OR rp.completed_at >= :from) AND (:to IS NULL OR rp.completed_at < :to) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE_FORMAT(rp.completed_at, '%Y-%m')", nativeQuery = true)
    List<MonthlyCountBucket> groupByMonthCompletedStatisticConditions(@Param("from") LocalDateTime from,
                                                                       @Param("to") LocalDateTime to,
                                                                       @Param("birthDateFrom") LocalDate birthDateFrom,
                                                                       @Param("birthDateTo") LocalDate birthDateTo);

    @Query("SELECT rp FROM ReadingProgress rp JOIN FETCH rp.rental r JOIN FETCH r.book " +
            "WHERE r.member.memberId = :memberId AND rp.lastReadAt IS NOT NULL " +
            "ORDER BY rp.lastReadAt DESC")
    List<ReadingProgress> findRecentByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
