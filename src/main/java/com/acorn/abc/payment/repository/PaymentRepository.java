package com.acorn.abc.payment.repository;

import com.acorn.abc.payment.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = :paymentStatus")
    long sumAmountByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    // 마이페이지 등급 달성률(PR #48 코드리뷰 대응 §3.3) — 회원의 등급 산정 시작일 이후 PAID 결제액 합계.
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.member.memberId = :memberId " +
            "AND p.paymentStatus = :paymentStatus AND p.paidAt >= :from")
    long sumAmountByMemberAndStatusSince(@Param("memberId") Long memberId,
                                          @Param("paymentStatus") String paymentStatus,
                                          @Param("from") LocalDateTime from);

    // 관리자 통계 결제 기간/나이대 조건부 집계(관리자 통계 확장 §3.1~§3.3)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = :paymentStatus " +
            "AND (:from IS NULL OR p.paidAt >= :from) AND (:to IS NULL OR p.paidAt < :to) " +
            "AND (:birthDateFrom IS NULL OR p.member.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR p.member.birthDate <= :birthDateTo)")
    long sumAmountByStatisticConditions(@Param("paymentStatus") String paymentStatus,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("birthDateFrom") LocalDate birthDateFrom,
                                         @Param("birthDateTo") LocalDate birthDateTo);

    interface DailyAmountBucket {
        LocalDate getBucketDate();
        Long getBucketAmount();
    }

    interface MonthlyAmountBucket {
        String getBucketMonth();
        Long getBucketAmount();
    }

    // 관리자 통계 추이(paymentTrend) — bucket 반복 조회 대신 기간 전체를 한 번에 GROUP BY로 집계한다(PR #42 리뷰 항목 2 반영).
    @Query(value = "SELECT DATE(p.paid_at) AS bucketDate, CAST(COALESCE(SUM(p.amount), 0) AS SIGNED) AS bucketAmount " +
            "FROM payment p JOIN member m ON p.member_id = m.member_id " +
            "WHERE p.payment_status = :paymentStatus " +
            "AND (:from IS NULL OR p.paid_at >= :from) AND (:to IS NULL OR p.paid_at < :to) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE(p.paid_at)", nativeQuery = true)
    List<DailyAmountBucket> groupByDayStatisticConditions(@Param("paymentStatus") String paymentStatus,
                                                           @Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to,
                                                           @Param("birthDateFrom") LocalDate birthDateFrom,
                                                           @Param("birthDateTo") LocalDate birthDateTo);

    @Query(value = "SELECT DATE_FORMAT(p.paid_at, '%Y-%m') AS bucketMonth, CAST(COALESCE(SUM(p.amount), 0) AS SIGNED) AS bucketAmount " +
            "FROM payment p JOIN member m ON p.member_id = m.member_id " +
            "WHERE p.payment_status = :paymentStatus " +
            "AND (:from IS NULL OR p.paid_at >= :from) AND (:to IS NULL OR p.paid_at < :to) " +
            "AND (:birthDateFrom IS NULL OR m.birth_date > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR m.birth_date <= :birthDateTo) " +
            "GROUP BY DATE_FORMAT(p.paid_at, '%Y-%m')", nativeQuery = true)
    List<MonthlyAmountBucket> groupByMonthStatisticConditions(@Param("paymentStatus") String paymentStatus,
                                                               @Param("from") LocalDateTime from,
                                                               @Param("to") LocalDateTime to,
                                                               @Param("birthDateFrom") LocalDate birthDateFrom,
                                                               @Param("birthDateTo") LocalDate birthDateTo);

    @Query("SELECT p FROM Payment p JOIN FETCH p.member WHERE p.paymentStatus = 'PAID' ORDER BY p.paidAt DESC")
    List<Payment> findRecentPayments(Pageable pageable);

    @Query(value = "SELECT p FROM Payment p JOIN FETCH p.rental r JOIN FETCH r.book " +
            "WHERE p.member.memberId = :memberId " +
            "AND (:from IS NULL OR p.paidAt >= :from) " +
            "AND (:to IS NULL OR p.paidAt < :to) " +
            "ORDER BY p.paidAt DESC",
            countQuery = "SELECT COUNT(p) FROM Payment p " +
            "WHERE p.member.memberId = :memberId " +
            "AND (:from IS NULL OR p.paidAt >= :from) " +
            "AND (:to IS NULL OR p.paidAt < :to)")
    Page<Payment> findMyPayments(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 관리자 회원 상세 PAYMENTS 탭(구현계획 §1.3) — paidAt DESC, paymentId DESC 고정.
    @Query(value = "SELECT p FROM Payment p JOIN FETCH p.rental r JOIN FETCH r.book WHERE p.member.memberId = :memberId " +
            "ORDER BY p.paidAt DESC, p.paymentId DESC",
            countQuery = "SELECT COUNT(p) FROM Payment p WHERE p.member.memberId = :memberId")
    Page<Payment> findByMemberIdForAdmin(@Param("memberId") Long memberId, Pageable pageable);

    long countByMember_MemberId(Long memberId);
}
