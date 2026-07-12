package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.review.repository.ReviewRepository;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 관리자 통계 확장 구현계획(2026-07-12) §5.1 기준 테스트.
 * Repository 호출 인자는 대부분 any()로 느슨하게 스텁하고, 기간/나이대 계산 정확성이 필요한
 * 테스트에서만 ArgumentCaptor로 실제 전달값을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private AdminStatisticService adminStatisticService;

    @BeforeEach
    void setUpDefaultStubs() {
        given(memberRepository.countByAgeBand(any(), any())).willReturn(5L);
        given(memberRepository.countByStatusAndAgeBand(anyString(), any(), any())).willReturn(1L);
        given(bookRepository.countByStatus("AVAILABLE")).willReturn(2L);
        given(bookRentalRepository.countByStatisticConditions(any(), any(), any(), any(), any())).willReturn(10L);
        given(readingProgressRepository.countCompletedByStatisticConditions(any(), any(), any(), any())).willReturn(4L);
        given(paymentRepository.sumAmountByStatisticConditions(anyString(), any(), any(), any(), any())).willReturn(19800L);
        given(reviewRepository.countByStatisticConditions(eq(Review.STATUS_ACTIVE), any(), any(), any(), any())).willReturn(6L);
        given(bookReportRepository.countByStatisticConditions(any(), any(), any(), any())).willReturn(2L);
        given(reviewReportRepository.countByStatisticConditions(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    void getStatistics_TOTAL_ALL_조합은_실시간으로_집계한_값과_최근_12개월_추이를_반환한다() {
        AdminStatisticResponse response = adminStatisticService.getStatistics("TOTAL", LocalDate.of(2026, 7, 15), null);

        assertThat(response.periodType()).isEqualTo("TOTAL");
        assertThat(response.ageBand()).isEqualTo("ALL");
        assertThat(response.totalMemberCount()).isEqualTo(5);
        assertThat(response.sanctionedMemberCount()).isEqualTo(1);
        assertThat(response.activeBookCount()).isEqualTo(2);
        assertThat(response.totalRentalCount()).isEqualTo(10);
        assertThat(response.freeRentalCount()).isEqualTo(10);
        assertThat(response.paidRentalCount()).isEqualTo(10);
        assertThat(response.totalReadBookCount()).isEqualTo(4);
        assertThat(response.totalPaymentAmount()).isEqualTo(19800L);
        assertThat(response.reviewCount()).isEqualTo(6L);
        assertThat(response.reportCount()).isEqualTo(3L);
        assertThat(response.rentalTrend()).hasSize(12);
        assertThat(response.paymentTrend()).hasSize(12);
        assertThat(response.completedReadTrend()).hasSize(12);
        assertThat(response.rentalTrend().get(11).label()).isEqualTo("2026-07");
        assertThat(response.rentalTrend().get(0).label()).isEqualTo("2025-08");
    }

    @Test
    void getStatistics_WEEKLY_ALL_조합은_해당_주_월요일부터_일요일까지를_기간으로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);
        LocalDate expectedMonday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        AdminStatisticResponse response = adminStatisticService.getStatistics("WEEKLY", baseDate, null);

        assertThat(response.periodStartDate()).isEqualTo(expectedMonday);
        assertThat(response.periodEndDate()).isEqualTo(expectedMonday.plusDays(6));
        assertThat(response.rentalTrend()).hasSize(7);
        assertThat(response.paymentTrend()).hasSize(7);
        assertThat(response.completedReadTrend()).hasSize(7);
        assertThat(response.rentalTrend().get(0).periodStartDate()).isEqualTo(expectedMonday);
    }

    @Test
    void getStatistics_MONTHLY_ALL_조합은_해당_월_전체를_기간으로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        AdminStatisticResponse response = adminStatisticService.getStatistics("MONTHLY", baseDate, null);

        assertThat(response.periodStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.periodEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.rentalTrend()).hasSize(31);
        assertThat(response.completedReadTrend()).hasSize(31);
    }

    @Test
    void getStatistics_YEARLY_ALL_조합은_해당_연도_전체와_12개월_추이를_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        AdminStatisticResponse response = adminStatisticService.getStatistics("YEARLY", baseDate, null);

        assertThat(response.periodStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.periodEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(response.rentalTrend()).hasSize(12);
        assertThat(response.rentalTrend().get(0).label()).isEqualTo("2026-01");
        assertThat(response.rentalTrend().get(11).label()).isEqualTo("2026-12");
    }

    @Test
    void getStatistics_MONTHLY_20S_조합은_나이대_범위를_계산해서_Repository에_전달한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        AdminStatisticResponse response = adminStatisticService.getStatistics("MONTHLY", baseDate, "20S");

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(memberRepository).countByAgeBand(fromCaptor.capture(), toCaptor.capture());

        // 20S: birthDate <= baseDate-20년(만 20세 이상), birthDate > baseDate-30년(만 29세 이하)
        assertThat(toCaptor.getValue()).isEqualTo(baseDate.minusYears(20));
        assertThat(fromCaptor.getValue()).isEqualTo(baseDate.minusYears(30));
        assertThat(response.ageBand()).isEqualTo("20S");
    }

    @Test
    void getStatistics_50_PLUS는_상한_나이_제한이_없다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        adminStatisticService.getStatistics("TOTAL", baseDate, "50_PLUS");

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(memberRepository).countByAgeBand(fromCaptor.capture(), toCaptor.capture());

        assertThat(fromCaptor.getValue()).isNull();
        assertThat(toCaptor.getValue()).isEqualTo(baseDate.minusYears(50));
    }

    @Test
    void getStatistics_생일이_기준일과_같은_날이면_그_나이대에_포함된다() {
        // baseDate 자체가 생일인 회원은 만 20세를 "막 채운" 경우이므로 20S(20~29세) 상한 경계에 포함돼야 한다.
        LocalDate baseDate = LocalDate.of(2026, 7, 15);
        LocalDate birthDateOnBoundary = baseDate.minusYears(20);

        adminStatisticService.getStatistics("TOTAL", baseDate, "20S");

        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(memberRepository).countByAgeBand(any(), toCaptor.capture());

        // toInclusive 조건은 birthDate <= toCaptor이므로, 생일 당일 출생자는 포함되어야 한다.
        assertThat(birthDateOnBoundary).isBeforeOrEqualTo(toCaptor.getValue());
    }

    @Test
    void getStatistics_완독수_기준으로_탄소_나무_계산값을_반환한다() {
        given(readingProgressRepository.countCompletedByStatisticConditions(any(), any(), any(), any())).willReturn(25L);

        AdminStatisticResponse response = adminStatisticService.getStatistics("TOTAL", LocalDate.of(2026, 7, 15), null);

        assertThat(response.totalReadBookCount()).isEqualTo(25L);
        assertThat(response.carbonSavedKg()).isEqualTo(50L);
        assertThat(response.treeSavedCount()).isEqualTo(1.0);
    }

    @Test
    void getTotalStatisticForDashboard_TOTAL_ALL_기준으로_집계한다() {
        AdminStatisticResponse response = adminStatisticService.getTotalStatisticForDashboard();

        assertThat(response.periodType()).isEqualTo("TOTAL");
        assertThat(response.ageBand()).isEqualTo("ALL");
        assertThat(response.rentalTrend()).hasSize(12);
    }
}
