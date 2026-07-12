package com.acorn.abc.coupon.service;

import com.acorn.abc.admin.coupon.dto.AdminCouponCreateRequest;
import com.acorn.abc.admin.coupon.dto.AdminCouponSummaryResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.coupon.repository.CouponRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.domain.Coupon;
import com.acorn.abc.payment.repository.MemberCouponRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    @InjectMocks
    private CouponService couponService;

    @Test
    void createCoupon_성공하면_감사로그를_기록한다() {
        AdminCouponCreateRequest request = new AdminCouponCreateRequest(
                "신규쿠폰", "DISCOUNT", 1000, "AMOUNT", 7, "ACTIVE");
        Coupon saved = mock(Coupon.class);
        given(saved.getCouponId()).willReturn(1L);
        given(couponRepository.save(any(Coupon.class))).willReturn(saved);

        Long couponId = couponService.createCoupon(request, 999L);

        assertThat(couponId).isEqualTo(1L);
        verify(adminAuditLogWriter).record(
                eq(999L), eq(AdminAuditLogActionType.COUPON_CREATE),
                eq(AdminAuditLogTargetType.COUPON), eq(1L), any(), any());
    }

    @Test
    void issueCoupon_성공하면_감사로그를_기록한다() {
        Coupon coupon = mock(Coupon.class);
        Member member = mock(Member.class);
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));

        int issuedCount = couponService.issueCoupon(1L, List.of(10L), 2, 999L);

        assertThat(issuedCount).isEqualTo(2);
        verify(adminAuditLogWriter).record(
                eq(999L), eq(AdminAuditLogActionType.COUPON_ISSUE),
                eq(AdminAuditLogTargetType.COUPON), eq(1L), any(), any());
    }

    @Test
    void getAdminCoupons_쿠폰상태별_집계를_배치조회해서_매핑한다() {
        Coupon coupon1 = coupon(1L, "쿠폰1");
        Coupon coupon2 = coupon(2L, "쿠폰2");
        given(couponRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(coupon1, coupon2)));
        given(memberCouponRepository.countByCouponIdsGroupedByStatus(List.of(1L, 2L)))
                .willReturn(List.of(
                        new CouponStatusCountRow(1L, "ISSUED", 3L),
                        new CouponStatusCountRow(1L, "USED", 2L),
                        new CouponStatusCountRow(2L, "EXPIRED", 1L)
                ));

        PageResponse<AdminCouponSummaryResponse> response =
                couponService.getAdminCoupons(null, null, 0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).issuedCount()).isEqualTo(5);
        assertThat(response.content().get(0).usedCount()).isEqualTo(2);
        assertThat(response.content().get(0).expiredCount()).isZero();
        assertThat(response.content().get(1).issuedCount()).isEqualTo(1);
        assertThat(response.content().get(1).expiredCount()).isEqualTo(1);
        verify(memberCouponRepository).countByCouponIdsGroupedByStatus(List.of(1L, 2L));
        verify(memberCouponRepository, never()).countByCoupon_CouponId(anyLong());
        verify(memberCouponRepository, never()).countByCoupon_CouponIdAndCouponStatus(anyLong(), anyString());
    }

    private Coupon coupon(Long id, String name) {
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponId()).willReturn(id);
        given(coupon.getCouponName()).willReturn(name);
        given(coupon.getCouponType()).willReturn("WELCOME");
        given(coupon.getBenefitValue()).willReturn(1000);
        given(coupon.getBenefitUnit()).willReturn("AMOUNT");
        given(coupon.getValidDays()).willReturn(14);
        given(coupon.getStatus()).willReturn("ACTIVE");
        return coupon;
    }

    private record CouponStatusCountRow(Long couponId, String couponStatus, Long count)
            implements MemberCouponRepository.CouponStatusCount {

        @Override
        public Long getCouponId() {
            return couponId;
        }

        @Override
        public String getCouponStatus() {
            return couponStatus;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }
}
