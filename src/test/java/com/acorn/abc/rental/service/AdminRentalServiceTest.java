package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.AdminRentalSummaryResponse;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminRentalServiceTest {

    @Mock private BookRentalRepository bookRentalRepository;
    @Mock private ReadingProgressRepository readingProgressRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminRentalService adminRentalService;

    @Test
    void getAdminRentals_잘못된_status면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminRentalService.getAdminRentals(null, null, "CANCELED", 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminRentals_size가_범위밖이면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminRentalService.getAdminRentals(null, null, null, 0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminRentals_결제와_진행률이_있으면_함께_조립해_반환한다() {
        BookRental rental = rental(1L, 10L, 100L);
        given(bookRentalRepository.searchForAdmin(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(rental), PageRequest.of(0, 10), 1));

        ReadingProgress progress = mock(ReadingProgress.class);
        given(progress.getRental()).willReturn(rental);
        given(progress.getProgression()).willReturn(new BigDecimal("0.50000"));
        given(readingProgressRepository.findByRental_RentalIdIn(List.of(1L))).willReturn(List.of(progress));

        Payment payment = mock(Payment.class);
        given(payment.getRental()).willReturn(rental);
        given(payment.getPaymentId()).willReturn(500L);
        given(payment.getAmount()).willReturn(9000);
        given(payment.getPaymentMethod()).willReturn("CARD");
        given(payment.getPaidAt()).willReturn(LocalDateTime.of(2026, 7, 22, 10, 0));
        given(paymentRepository.findByPaymentStatusAndRental_RentalIdIn("PAID", List.of(1L))).willReturn(List.of(payment));

        PageResponse<AdminRentalSummaryResponse> response = adminRentalService.getAdminRentals(null, null, null, 0, 10);

        assertThat(response.content()).hasSize(1);
        AdminRentalSummaryResponse summary = response.content().get(0);
        assertThat(summary.rentalId()).isEqualTo(1L);
        assertThat(summary.progression()).isEqualByComparingTo("0.50000");
        assertThat(summary.payment()).isNotNull();
        assertThat(summary.payment().paymentId()).isEqualTo(500L);
    }

    @Test
    void getAdminRentals_결제가_없으면_payment는_null이다() {
        BookRental rental = rental(2L, 20L, 200L);
        given(bookRentalRepository.searchForAdmin(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(rental), PageRequest.of(0, 10), 1));
        given(readingProgressRepository.findByRental_RentalIdIn(List.of(2L))).willReturn(List.of());
        given(paymentRepository.findByPaymentStatusAndRental_RentalIdIn(anyString(), any())).willReturn(List.of());

        PageResponse<AdminRentalSummaryResponse> response = adminRentalService.getAdminRentals(null, null, "READY", 0, 10);

        assertThat(response.content().get(0).payment()).isNull();
        assertThat(response.content().get(0).progression()).isNull();
    }

    private BookRental rental(Long rentalId, Long memberId, Long bookId) {
        BookRental rental = mock(BookRental.class);
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(memberId);
        given(member.getName()).willReturn("회원" + memberId);
        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(bookId);
        given(book.getTitle()).willReturn("도서" + bookId);

        given(rental.getRentalId()).willReturn(rentalId);
        given(rental.getMember()).willReturn(member);
        given(rental.getBook()).willReturn(book);
        given(rental.getRentalStatus()).willReturn("READY");
        given(rental.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 20, 9, 0));
        return rental;
    }
}
