package com.acorn.abc.coupon.service;

import com.acorn.abc.admin.coupon.dto.AdminCouponCreateRequest;
import com.acorn.abc.admin.coupon.dto.AdminCouponSummaryResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.coupon.dto.MyCouponResponse;
import com.acorn.abc.coupon.repository.CouponRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.domain.Coupon;
import com.acorn.abc.payment.domain.MemberCoupon;
import com.acorn.abc.payment.repository.MemberCouponRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private static final int EXPIRING_SOON_DAYS = 3;

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;

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

        List<Coupon> coupons = result.getContent();
        Map<Long, CouponCounts> countsByCouponId = loadCouponCounts(coupons);
        List<AdminCouponSummaryResponse> content = coupons.stream()
                .map(coupon -> toAdminSummary(coupon, countsByCouponId.getOrDefault(coupon.getCouponId(), CouponCounts.empty())))
                .toList();
        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    public Long createCoupon(AdminCouponCreateRequest request, Long adminMemberId) {
        Coupon coupon = Coupon.create(request.couponName(), request.couponType(), request.benefitValue(),
                request.benefitUnit(), request.validDays(), request.status());
        Long couponId = couponRepository.save(coupon).getCouponId();
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COUPON_CREATE,
                AdminAuditLogTargetType.COUPON, couponId, null,
                Map.of("couponName", request.couponName(), "couponType", request.couponType()));
        return couponId;
    }

    public int issueCoupon(Long couponId, List<Long> memberIds, int quantity, Long adminMemberId) {
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
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.COUPON_ISSUE,
                AdminAuditLogTargetType.COUPON, couponId, null,
                Map.of("issuedCount", issuedCount, "memberCount", memberIds.size(), "quantityPerMember", quantity));
        return issuedCount;
    }

    private Map<Long, CouponCounts> loadCouponCounts(List<Coupon> coupons) {
        if (coupons.isEmpty()) {
            return Map.of();
        }

        List<Long> couponIds = coupons.stream()
                .map(Coupon::getCouponId)
                .toList();
        Map<Long, CouponCounts> countsByCouponId = new HashMap<>();
        memberCouponRepository.countByCouponIdsGroupedByStatus(couponIds)
                .forEach(row -> countsByCouponId
                        .computeIfAbsent(row.getCouponId(), ignored -> new CouponCounts())
                        .add(row.getCouponStatus(), row.getCount()));
        return countsByCouponId;
    }

    private AdminCouponSummaryResponse toAdminSummary(Coupon coupon, CouponCounts counts) {
        return new AdminCouponSummaryResponse(
                coupon.getCouponId(), coupon.getCouponName(), coupon.getCouponType(),
                coupon.getBenefitValue(), coupon.getBenefitUnit(), coupon.getValidDays(), coupon.getStatus(),
                counts.totalCount, counts.usedCount, counts.expiredCount);
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

    private static class CouponCounts {
        private long totalCount;
        private long usedCount;
        private long expiredCount;

        static CouponCounts empty() {
            return new CouponCounts();
        }

        void add(String status, Long count) {
            long value = count == null ? 0 : count;
            totalCount += value;
            if ("USED".equals(status)) {
                usedCount += value;
            } else if ("EXPIRED".equals(status)) {
                expiredCount += value;
            }
        }
    }
}
