package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.ReadingProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    Optional<ReadingProgress> findByRental_RentalId(Long rentalId);

    // 관리자 대여 현황 조회(API-ADMIN-RENTAL-001, 2026-07-22 구현계획 §2.3) — 페이지 조회 후 rental id 목록으로
    // 진행률을 한 번에 bulk 조회한다(N+1 방지).
    List<ReadingProgress> findByRental_RentalIdIn(List<Long> rentalIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rp FROM ReadingProgress rp WHERE rp.rental.rentalId = :rentalId")
    Optional<ReadingProgress> findByRentalIdForUpdate(@Param("rentalId") Long rentalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReadingProgress rp SET rp.epubVersion = :epubVersion, rp.locationCfi = null, "
            + "rp.chapterHref = null, rp.spineIndex = null, rp.progression = 0, rp.lastReadAt = null, "
            + "rp.completedYn = false, rp.completedAt = null, rp.revision = rp.revision + 1 "
            + "WHERE rp.rental.book.bookId = :bookId")
    int resetForEpubVersion(@Param("bookId") Long bookId, @Param("epubVersion") int epubVersion);

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

    // 회원용 독서 통계 완독 수(회원용독서통계 구현계획 §5.2). 본인 회원 ID + 기간 조건.
    @Query("SELECT COUNT(rp) FROM ReadingProgress rp WHERE rp.rental.member.memberId = :memberId AND rp.completedYn = true " +
            "AND (:from IS NULL OR rp.completedAt >= :from) AND (:to IS NULL OR rp.completedAt < :to)")
    long countCompletedByMemberAndPeriod(@Param("memberId") Long memberId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    // 회원용 독서 통계 "읽은 페이지"(구현계획 §4.2 B1 확정) — last_read_at이 기간 안에 있는 행의
    // read_page_count(누적 최대 도달 페이지)를 합산한 근사치다. 정확한 순수 독서량이 아니다.
    @Query("SELECT COALESCE(SUM(rp.readPageCount), 0) FROM ReadingProgress rp "
            + "WHERE rp.rental.member.memberId = :memberId "
            + "AND (:from IS NULL OR rp.lastReadAt >= :from) AND (:to IS NULL OR rp.lastReadAt < :to)")
    long sumReadPageCountByMemberAndPeriod(@Param("memberId") Long memberId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    // 회원용 독서 통계 완독 추이(readingTrend, 구현계획 §4.3 — completedReadTrend와 동일 개념 재사용). 본인 회원 ID만 필터링.
    @Query(value = "SELECT DATE(rp.completed_at) AS bucketDate, COUNT(*) AS bucketCount " +
            "FROM reading_progress rp JOIN book_rental r ON rp.rental_id = r.rental_id " +
            "WHERE r.member_id = :memberId AND rp.completed_yn = 1 " +
            "AND (:from IS NULL OR rp.completed_at >= :from) AND (:to IS NULL OR rp.completed_at < :to) " +
            "GROUP BY DATE(rp.completed_at)", nativeQuery = true)
    List<DailyCountBucket> groupByDayCompletedByMember(@Param("memberId") Long memberId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE_FORMAT(rp.completed_at, '%Y-%m') AS bucketMonth, COUNT(*) AS bucketCount " +
            "FROM reading_progress rp JOIN book_rental r ON rp.rental_id = r.rental_id " +
            "WHERE r.member_id = :memberId AND rp.completed_yn = 1 " +
            "AND (:from IS NULL OR rp.completed_at >= :from) AND (:to IS NULL OR rp.completed_at < :to) " +
            "GROUP BY DATE_FORMAT(rp.completed_at, '%Y-%m')", nativeQuery = true)
    List<MonthlyCountBucket> groupByMonthCompletedByMember(@Param("memberId") Long memberId,
                                                             @Param("from") LocalDateTime from,
                                                             @Param("to") LocalDateTime to);
}
