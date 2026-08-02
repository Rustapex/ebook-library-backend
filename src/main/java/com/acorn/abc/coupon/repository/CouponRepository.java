package com.acorn.abc.coupon.repository;

import com.acorn.abc.payment.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Page<Coupon> findByStatus(String status, Pageable pageable);

    Page<Coupon> findByCouponType(String couponType, Pageable pageable);

    Page<Coupon> findByStatusAndCouponType(String status, String couponType, Pageable pageable);
}
