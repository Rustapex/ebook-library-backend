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
}
