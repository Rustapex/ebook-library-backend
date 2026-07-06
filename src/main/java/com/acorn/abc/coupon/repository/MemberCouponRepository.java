package com.acorn.abc.coupon.repository;

import com.acorn.abc.payment.domain.MemberCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    Page<MemberCoupon> findByMember_MemberId(Long memberId, Pageable pageable);

    Page<MemberCoupon> findByMember_MemberIdAndCouponStatus(Long memberId, String couponStatus, Pageable pageable);

    long countByCoupon_CouponId(Long couponId);

    long countByCoupon_CouponIdAndCouponStatus(Long couponId, String couponStatus);
}
