package com.acorn.abc.payment.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.payment.domain.CouponUsage;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.dto.AdminPaymentSummaryResponse;
import com.acorn.abc.payment.repository.CouponUsageRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.domain.BookRental;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CouponUsageRepository couponUsageRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private AdminPaymentService adminPaymentService;

    @Test
    void getAdminPayments_size가_범위밖이면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminPaymentService.getAdminPayments(null, null, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminPayments_쿠폰할인과_포인트차감을_함께_조립해_반환한다() {
        Payment payment = payment(1L, 100L);
        given(paymentRepository.searchForAdmin(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1));

        CouponUsage couponUsage = mock(CouponUsage.class);
        given(couponUsage.getPayment()).willReturn(payment);
        given(couponUsage.getDiscountAmount()).willReturn(1000);
        given(couponUsageRepository.findByPayment_PaymentIdIn(List.of(1L))).willReturn(List.of(couponUsage));

        PointHistory pointHistory = mock(PointHistory.class);
        given(pointHistory.getPayment()).willReturn(payment);
        given(pointHistory.getPointType()).willReturn("PAYMENT_USE");
        given(pointHistory.getPointAmount()).willReturn(-500);
        given(pointHistoryRepository.findByPayment_PaymentIdIn(List.of(1L))).willReturn(List.of(pointHistory));

        PageResponse<AdminPaymentSummaryResponse> response =
                adminPaymentService.getAdminPayments("회원", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 22), 0, 10);

        AdminPaymentSummaryResponse summary = response.content().get(0);
        assertThat(summary.couponDiscountAmount()).isEqualTo(1000);
        assertThat(summary.pointUsedAmount()).isEqualTo(500);
    }

    @Test
    void getAdminPayments_쿠폰_포인트_사용_내역이_없으면_0으로_반환한다() {
        Payment payment = payment(2L, 200L);
        given(paymentRepository.searchForAdmin(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1));
        given(couponUsageRepository.findByPayment_PaymentIdIn(List.of(2L))).willReturn(List.of());
        given(pointHistoryRepository.findByPayment_PaymentIdIn(List.of(2L))).willReturn(List.of());

        PageResponse<AdminPaymentSummaryResponse> response = adminPaymentService.getAdminPayments(null, null, null, 0, 10);

        AdminPaymentSummaryResponse summary = response.content().get(0);
        assertThat(summary.couponDiscountAmount()).isZero();
        assertThat(summary.pointUsedAmount()).isZero();
    }

    private Payment payment(Long paymentId, Long memberId) {
        Payment payment = mock(Payment.class);
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(memberId);
        given(member.getName()).willReturn("회원" + memberId);
        BookRental rental = mock(BookRental.class);
        given(rental.getRentalId()).willReturn(memberId * 10);
        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(memberId * 100);
        given(book.getTitle()).willReturn("도서" + memberId);
        given(rental.getBook()).willReturn(book);

        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getMember()).willReturn(member);
        given(payment.getRental()).willReturn(rental);
        given(payment.getPaymentMethod()).willReturn("CARD");
        given(payment.getOriginalAmount()).willReturn(10000);
        given(payment.getAmount()).willReturn(9000);
        given(payment.getPaidAt()).willReturn(LocalDateTime.of(2026, 7, 22, 10, 0));
        return payment;
    }
}
