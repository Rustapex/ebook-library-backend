package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminStatisticServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookRentalRepository bookRentalRepository;

    @Mock
    private ReadingProgressRepository readingProgressRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminStatisticService adminStatisticService;

    @Test
    void getStatistics_TOTAL_ALL_조합은_실시간으로_집계한_값을_반환한다() {
        given(memberRepository.count()).willReturn(5L);
        given(memberRepository.countByStatus("SANCTIONED")).willReturn(1L);
        given(bookRepository.countByStatus("AVAILABLE")).willReturn(2L);
        given(bookRentalRepository.count()).willReturn(10L);
        given(bookRentalRepository.countByBook_RentalType("FREE")).willReturn(7L);
        given(bookRentalRepository.countByBook_RentalType("PAID")).willReturn(3L);
        given(readingProgressRepository.countByCompletedYn(true)).willReturn(4L);
        given(paymentRepository.sumAmountByPaymentStatus("PAID")).willReturn(19800L);

        AdminStatisticResponse response = adminStatisticService.getStatistics("TOTAL", null, null);

        assertThat(response.periodType()).isEqualTo("TOTAL");
        assertThat(response.ageBand()).isEqualTo("ALL");
        assertThat(response.totalMemberCount()).isEqualTo(5);
        assertThat(response.sanctionedMemberCount()).isEqualTo(1);
        assertThat(response.activeBookCount()).isEqualTo(2);
        assertThat(response.totalRentalCount()).isEqualTo(10);
        assertThat(response.freeRentalCount()).isEqualTo(7);
        assertThat(response.paidRentalCount()).isEqualTo(3);
        assertThat(response.totalReadBookCount()).isEqualTo(4);
        assertThat(response.totalPaymentAmount()).isEqualTo(19800L);
        // review_count/report_count는 review 패키지에 Repository가 없어(§4-4) 항상 0이다.
        assertThat(response.reviewCount()).isZero();
        assertThat(response.reportCount()).isZero();
    }

    @Test
    void getStatistics_WEEKLY처럼_아직_지원하지_않는_periodType은_COMMON_NOT_FOUND() {
        assertThatThrownBy(() -> adminStatisticService.getStatistics("WEEKLY", null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void getStatistics_TOTAL이어도_ageBand이_ALL이_아니면_COMMON_NOT_FOUND() {
        assertThatThrownBy(() -> adminStatisticService.getStatistics("TOTAL", null, "10S"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }
}
