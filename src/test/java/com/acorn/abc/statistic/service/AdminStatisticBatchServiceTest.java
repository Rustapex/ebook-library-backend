package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.statistic.domain.AdminStatistic;
import com.acorn.abc.statistic.repository.AdminStatisticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminStatisticBatchServiceTest {

    @Mock
    private AdminStatisticRepository adminStatisticRepository;

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
    private AdminStatisticBatchService adminStatisticBatchService;

    @Test
    void refreshTotalStatistics_기존_행이_없으면_신규로_생성해서_저장한다() {
        given(memberRepository.count()).willReturn(100L);
        given(memberRepository.countByStatus("SANCTIONED")).willReturn(3L);
        given(bookRepository.countByStatus("AVAILABLE")).willReturn(50L);
        given(bookRentalRepository.count()).willReturn(200L);
        given(bookRentalRepository.countByBook_RentalType("FREE")).willReturn(120L);
        given(bookRentalRepository.countByBook_RentalType("PAID")).willReturn(80L);
        given(readingProgressRepository.countByCompletedYn(true)).willReturn(40L);
        given(paymentRepository.sumAmountByPaymentStatus("PAID")).willReturn(500_000L);
        given(adminStatisticRepository.findByPeriodTypeAndPeriodStartDateAndAgeBand(
                "TOTAL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, "ALL"))
                .willReturn(Optional.empty());

        adminStatisticBatchService.refreshTotalStatistics();

        ArgumentCaptor<AdminStatistic> captor = ArgumentCaptor.forClass(AdminStatistic.class);
        verify(adminStatisticRepository).save(captor.capture());
        AdminStatistic saved = captor.getValue();

        assertThat(saved.getPeriodType()).isEqualTo("TOTAL");
        assertThat(saved.getAgeBand()).isEqualTo("ALL");
        assertThat(saved.getPeriodStartDate()).isEqualTo(AdminStatisticBatchService.TOTAL_PERIOD_START_DATE);
        assertThat(saved.getTotalMemberCount()).isEqualTo(100);
        assertThat(saved.getSanctionedMemberCount()).isEqualTo(3);
        assertThat(saved.getActiveBookCount()).isEqualTo(50);
        assertThat(saved.getTotalRentalCount()).isEqualTo(200);
        assertThat(saved.getFreeRentalCount()).isEqualTo(120);
        assertThat(saved.getPaidRentalCount()).isEqualTo(80);
        assertThat(saved.getTotalReadBookCount()).isEqualTo(40);
        assertThat(saved.getTotalPaymentAmount()).isEqualTo(500_000L);
        // review_count/report_count는 review 패키지에 Repository가 없어(§4-4) 0으로 고정된다.
        assertThat(saved.getReviewCount()).isZero();
        assertThat(saved.getReportCount()).isZero();
    }

    @Test
    void refreshTotalStatistics_기존_행이_있으면_같은_행을_갱신한다() {
        AdminStatistic existing = AdminStatistic.create(
                "TOTAL", "ALL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, java.time.LocalDate.now().minusDays(1));
        given(adminStatisticRepository.findByPeriodTypeAndPeriodStartDateAndAgeBand(
                "TOTAL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, "ALL"))
                .willReturn(Optional.of(existing));
        given(memberRepository.count()).willReturn(10L);
        given(memberRepository.countByStatus(any())).willReturn(0L);
        given(bookRepository.countByStatus(any())).willReturn(0L);
        given(bookRentalRepository.count()).willReturn(0L);
        given(bookRentalRepository.countByBook_RentalType(any())).willReturn(0L);
        given(readingProgressRepository.countByCompletedYn(any())).willReturn(0L);
        given(paymentRepository.sumAmountByPaymentStatus(any())).willReturn(0L);

        adminStatisticBatchService.refreshTotalStatistics();

        ArgumentCaptor<AdminStatistic> captor = ArgumentCaptor.forClass(AdminStatistic.class);
        verify(adminStatisticRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getTotalMemberCount()).isEqualTo(10);
    }
}
