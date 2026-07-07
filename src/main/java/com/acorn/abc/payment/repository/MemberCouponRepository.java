package com.acorn.abc.payment.repository;

import com.acorn.abc.payment.domain.MemberCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    Optional<MemberCoupon> findByMemberCouponIdAndMember_MemberId(Long memberCouponId, Long memberId);

    Page<MemberCoupon> findByMember_MemberId(Long memberId, Pageable pageable);

    Page<MemberCoupon> findByMember_MemberIdAndCouponStatus(Long memberId, String couponStatus, Pageable pageable);

    long countByCoupon_CouponId(Long couponId);

    long countByCoupon_CouponIdAndCouponStatus(Long couponId, String couponStatus);

    @Query("SELECT mc.coupon.couponId AS couponId, mc.couponStatus AS couponStatus, COUNT(mc) AS count "
            + "FROM MemberCoupon mc "
            + "WHERE mc.coupon.couponId IN :couponIds "
            + "GROUP BY mc.coupon.couponId, mc.couponStatus")
    List<CouponStatusCount> countByCouponIdsGroupedByStatus(@Param("couponIds") Collection<Long> couponIds);

    interface CouponStatusCount {
        Long getCouponId();

        String getCouponStatus();

        Long getCount();
    }
}
