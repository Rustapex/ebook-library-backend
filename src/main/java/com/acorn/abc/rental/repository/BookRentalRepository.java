package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.BookRental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface BookRentalRepository extends JpaRepository<BookRental, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BookRental r JOIN FETCH r.member JOIN FETCH r.book WHERE r.rentalId = :rentalId")
    Optional<BookRental> findByIdForUpdate(@Param("rentalId") Long rentalId);

    // book_rental은 (member_id, book_id) UNIQUE 제약이 없어 재대여 시 행이 여러 개 쌓일 수 있다 -> 최신 1건만 조회
    // createdAt이 동률일 수 있어 rentalId desc를 tie-breaker로 추가해 조회 결과를 결정적으로 만든다
    Optional<BookRental> findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(Long memberId, Long bookId);

    long countByBook_RentalType(String rentalType);

    // 마이페이지 전체 대여 수(PR #48 코드리뷰 대응 §3.2)
    long countByMember_MemberId(Long memberId);

    // 관리자 통계 기간/나이대/대여유형 조건부 집계(관리자 통계 확장 §3.1~§3.3). from/to/rentalType/birthDate 조건은 null이면 필터 미적용.
    @Query("SELECT COUNT(r) FROM BookRental r WHERE " +
            "(:from IS NULL OR r.createdAt >= :from) AND (:to IS NULL OR r.createdAt < :to) " +
            "AND (:rentalType IS NULL OR r.book.rentalType = :rentalType) " +
            "AND (:birthDateFrom IS NULL OR r.member.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR r.member.birthDate <= :birthDateTo)")
    long countByStatisticConditions(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("rentalType") String rentalType,
                                     @Param("birthDateFrom") LocalDate birthDateFrom,
                                     @Param("birthDateTo") LocalDate birthDateTo);

    // 회원용 독서 통계 기간 조건부 집계(회원용독서통계 구현계획 §5.2). 본인 회원 ID로만 필터링한다.
    @Query("SELECT COUNT(r) FROM BookRental r WHERE r.member.memberId = :memberId " +
            "AND (:from IS NULL OR r.createdAt >= :from) AND (:to IS NULL OR r.createdAt < :to)")
    long countByMemberAndPeriod(@Param("memberId") Long memberId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    interface DailyCountBucket {
        LocalDate getBucketDate();
        Long getBucketCount();
    }

    interface MonthlyCountBucket {
        String getBucketMonth();
        Long getBucketCount();
    }

    // 관리자 통계 추이(rentalTrend) — bucket마다 반복 조회하지 않고 기간 전체를 한 번에 GROUP BY로 집계한다(PR #42 리뷰 항목 2 반영).
    @Query(value = "SELECT DATE(r.created_at) AS bucketDate, COUNT(*) AS bucketCount " +
            "FROM book_rental r JOIN book b ON r.book_id = b.book_id JOIN member m ON r.member_id = m.member_id " +
            "WHERE (:from IS NULL OR r.created_at >= :from) AND (:to IS NULL OR r.created_at < :to) " +
            "AND (:rentalType IS NULL OR b.rental_type = :rentalType) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE(r.created_at)", nativeQuery = true)
    List<DailyCountBucket> groupByDayStatisticConditions(@Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to,
                                                          @Param("rentalType") String rentalType,
                                                          @Param("birthDateFrom") LocalDate birthDateFrom,
                                                          @Param("birthDateTo") LocalDate birthDateTo);

    @Query(value = "SELECT DATE_FORMAT(r.created_at, '%Y-%m') AS bucketMonth, COUNT(*) AS bucketCount " +
            "FROM book_rental r JOIN book b ON r.book_id = b.book_id JOIN member m ON r.member_id = m.member_id " +
            "WHERE (:from IS NULL OR r.created_at >= :from) AND (:to IS NULL OR r.created_at < :to) " +
            "AND (:rentalType IS NULL OR b.rental_type = :rentalType) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE_FORMAT(r.created_at, '%Y-%m')", nativeQuery = true)
    List<MonthlyCountBucket> groupByMonthStatisticConditions(@Param("from") LocalDateTime from,
                                                              @Param("to") LocalDateTime to,
                                                              @Param("rentalType") String rentalType,
                                                              @Param("birthDateFrom") LocalDate birthDateFrom,
                                                              @Param("birthDateTo") LocalDate birthDateTo);

    @Query(value = "SELECT r FROM BookRental r JOIN FETCH r.book " +
            "WHERE r.member.memberId = :memberId AND (:status IS NULL OR r.rentalStatus = :status) " +
            "ORDER BY r.createdAt DESC, r.rentalId DESC",
            countQuery = "SELECT COUNT(r) FROM BookRental r " +
            "WHERE r.member.memberId = :memberId AND (:status IS NULL OR r.rentalStatus = :status)")
    Page<BookRental> findMyRentals(@Param("memberId") Long memberId, @Param("status") String status, Pageable pageable);
}
