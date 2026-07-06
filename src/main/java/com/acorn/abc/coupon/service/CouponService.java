package com.acorn.abc.coupon.service;

import com.acorn.abc.admin.coupon.dto.AdminCouponCreateRequest;
import com.acorn.abc.admin.coupon.dto.AdminCouponSummaryResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.coupon.dto.MyCouponResponse;
import com.acorn.abc.coupon.repository.CouponRepository;
import com.acorn.abc.coupon.repository.MemberCouponRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.domain.Coupon;
import com.acorn.abc.payment.domain.MemberCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private static final int EXPIRING_SOON_DAYS = 3;

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<MyCouponResponse> getMyCoupons(Long memberId, String status, Long usableForBookId, int page, int size) {
        // usableForBookId는 도서별 쿠폰 제한이 schema상 없어 "사용 가능(ISSUED)" 필터로만 처리한다.
        String effectiveStatus = (usableForBookId != null) ? "ISSUED" : status;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
        Page<MemberCoupon> result = (effectiveStatus == null || effectiveStatus.isBlank())
                ? memberCouponRepository.findByMember_MemberId(memberId, pageable)
                : memberCouponRepository.findByMember_MemberIdAndCouponStatus(memberId, effectiveStatus, pageable);

        LocalDateTime expiringThreshold = LocalDateTime.now().plusDays(EXPIRING_SOON_DAYS);
        List<MyCouponResponse> content = result.getContent().stream()
                .map(mc -> toResponse(mc, expiringThreshold))
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCouponSummaryResponse> getAdminCoupons(String status, String couponType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasType = couponType != null && !couponType.isBlank();

        Page<Coupon> result;
        if (hasStatus && hasType) {
            result = couponRepository.findByStatusAndCouponType(status, couponType, pageable);
        } else if (hasStatus) {
            result = couponRepository.findByStatus(status, pageable);
        } else if (hasType) {
            result = couponRepository.findByCouponType(couponType, pageable);
        } else {
            result = couponRepository.findAll(pageable);
        }

        List<AdminCouponSummaryResponse> content = result.getContent().stream()
                .map(this::toAdminSummary)
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    public Long createCoupon(AdminCouponCreateRequest request) {
        Coupon coupon = Coupon.create(request.couponName(), request.couponType(), request.benefitValue(),
                request.benefitUnit(), request.validDays(), request.status());
        return couponRepository.save(coupon).getCouponId();
    }

    public int issueCoupon(Long couponId, List<Long> memberIds, int quantity) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        int issuedCount = 0;
        for (Long memberId : memberIds) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            for (int i = 0; i < quantity; i++) {
                memberCouponRepository.save(MemberCoupon.create(member, coupon));
                issuedCount++;
            }
        }
        return issuedCount;
    }

    private AdminCouponSummaryResponse toAdminSummary(Coupon coupon) {
        long issuedCount = memberCouponRepository.countByCoupon_CouponId(coupon.getCouponId());
        long usedCount = memberCouponRepository.countByCoupon_CouponIdAndCouponStatus(coupon.getCouponId(), "USED");
        long expiredCount = memberCouponRepository.countByCoupon_CouponIdAndCouponStatus(coupon.getCouponId(), "EXPIRED");
        return new AdminCouponSummaryResponse(
                coupon.getCouponId(), coupon.getCouponName(), coupon.getCouponType(),
                coupon.getBenefitValue(), coupon.getBenefitUnit(), coupon.getValidDays(), coupon.getStatus(),
                issuedCount, usedCount, expiredCount);
    }

    private MyCouponResponse toResponse(MemberCoupon mc, LocalDateTime expiringThreshold) {
        boolean expiringYn = "ISSUED".equals(mc.getCouponStatus()) && mc.getExpiresAt().isBefore(expiringThreshold);
        return new MyCouponResponse(
                mc.getMemberCouponId(),
                mc.getCoupon().getCouponName(),
                mc.getCoupon().getCouponType(),
                mc.getCoupon().getBenefitValue(),
                mc.getCoupon().getBenefitUnit(),
                mc.getCouponStatus(),
                mc.getIssuedAt(),
                mc.getExpiresAt(),
                mc.getUsedAt(),
                expiringYn
        );
    }
}
