package com.acorn.abc.payment.repository;

import com.acorn.abc.payment.domain.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    Optional<CouponUsage> findByPayment_PaymentId(Long paymentId);

    // 관리자 결제 목록 조회(API-ADMIN-PAYMENT-001, 2026-07-22 구현계획 §2.3) — payment id 목록으로
    // 쿠폰 할인액을 한 번에 bulk 조회한다(N+1 방지).
    List<CouponUsage> findByPayment_PaymentIdIn(List<Long> paymentIds);
}
