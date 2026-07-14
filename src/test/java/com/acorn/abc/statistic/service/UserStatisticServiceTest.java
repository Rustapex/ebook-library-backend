package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.repository.ReviewRepository;
import com.acorn.abc.statistic.dto.UserReadingStatisticResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 회원용독서통계 구현계획(2026-07-13) §5.3/§5.5 기준 테스트.
 * AdminStatisticServiceTest와 동일한 패턴(느슨한 스텁 + fail-fast 검증)을 따른다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserStatisticServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private BookRentalRepository bookRentalRepository;

    @Mock
    private ReadingProgressRepository readingProgressRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private FavoriteBookRepository favoriteBookRepository;

    private UserStatisticService userStatisticService;

    @BeforeEach
    void setUpDefaultStubs() {
        // 관리자API+사용자통계 전체잔여작업 §3.1-2: 기간/추이 계산이 공통 컴포넌트로 분리됐다.
        // 이 컴포넌트들은 순수 계산 로직(외부 의존성 없음)이라 mock 대신 실제 인스턴스를 사용한다.
        userStatisticService = new UserStatisticService(
                bookRentalRepository, readingProgressRepository, reviewRepository, favoriteBookRepository,
                new StatisticPeriodResolver(), new TrendBucketCalculator());

        given(bookRentalRepository.countByMemberAndPeriod(anyLong(), any(), any())).willReturn(3L);
        given(readingProgressRepository.countCompletedByMemberAndPeriod(anyLong(), any(), any())).willReturn(2L);
        given(readingProgressRepository.sumReadPageCountByMemberAndPeriod(anyLong(), any(), any())).willReturn(150L);
        given(reviewRepository.countByMemberAndPeriod(anyLong(), eq(Review.STATUS_ACTIVE), any(), any())).willReturn(1L);
        given(favoriteBookRepository.countByMember_MemberId(anyLong())).willReturn(4L);
    }

    @Test
    void getStatistics_TOTAL은_실시간으로_집계한_값과_최근_12개월_추이를_반환한다() {
        UserReadingStatisticResponse response =
                userStatisticService.getStatistics(MEMBER_ID, "TOTAL", LocalDate.of(2026, 7, 15));

        assertThat(response.periodType()).isEqualTo("TOTAL");
        assertThat(response.rentalCount()).isEqualTo(3L);
        assertThat(response.readBookCount()).isEqualTo(2L);
        assertThat(response.readPageCount()).isEqualTo(150L);
        assertThat(response.reviewCount()).isEqualTo(1L);
        assertThat(response.favoriteCount()).isEqualTo(4L);
        assertThat(response.readingTrend()).hasSize(12);
        assertThat(response.readingTrend().get(11).label()).isEqualTo("2026-07");

        // PR #45 리뷰 반영: memberId가 repository 호출까지 그대로 전달되는지 확인한다(다른 회원 데이터가
        // 섞이지 않는다는 계약의 핵심이므로, 어느 하나라도 회귀가 생기면 이 검증이 잡아야 한다).
        verify(bookRentalRepository).countByMemberAndPeriod(eq(MEMBER_ID), any(), any());
    }

    @Test
    void getStatistics_WEEKLY는_해당_주_월요일부터_일요일까지를_기간으로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);
        LocalDate expectedMonday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        UserReadingStatisticResponse response = userStatisticService.getStatistics(MEMBER_ID, "WEEKLY", baseDate);

        assertThat(response.periodStartDate()).isEqualTo(expectedMonday);
        assertThat(response.periodEndDate()).isEqualTo(expectedMonday.plusDays(6));
        assertThat(response.readingTrend()).hasSize(7);
    }

    @Test
    void getStatistics_MONTHLY는_해당_월_전체를_기간으로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        UserReadingStatisticResponse response = userStatisticService.getStatistics(MEMBER_ID, "MONTHLY", baseDate);

        assertThat(response.periodStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.periodEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.readingTrend()).hasSize(31);
    }

    @Test
    void getStatistics_YEARLY는_해당_연도_전체와_12개월_추이를_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 7, 15);

        UserReadingStatisticResponse response = userStatisticService.getStatistics(MEMBER_ID, "YEARLY", baseDate);

        assertThat(response.periodStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.periodEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(response.readingTrend()).hasSize(12);
        assertThat(response.readingTrend().get(0).label()).isEqualTo("2026-01");
        assertThat(response.readingTrend().get(11).label()).isEqualTo("2026-12");
    }

    @Test
    void getStatistics_완독수_기준으로_탄소_나무_계산값을_반환한다() {
        given(readingProgressRepository.countCompletedByMemberAndPeriod(anyLong(), any(), any())).willReturn(25L);

        UserReadingStatisticResponse response =
                userStatisticService.getStatistics(MEMBER_ID, "TOTAL", LocalDate.of(2026, 7, 15));

        assertThat(response.readBookCount()).isEqualTo(25L);
        assertThat(response.carbonSavedKg()).isEqualTo(50L);
        assertThat(response.treeSavedCount()).isEqualTo(1.0);
    }

    @Test
    void getStatistics_즐겨찾기_수는_periodType과_무관하게_현재_상태를_그대로_반환한다() {
        given(favoriteBookRepository.countByMember_MemberId(MEMBER_ID)).willReturn(7L);

        UserReadingStatisticResponse total = userStatisticService.getStatistics(MEMBER_ID, "TOTAL", LocalDate.of(2026, 7, 15));
        UserReadingStatisticResponse weekly = userStatisticService.getStatistics(MEMBER_ID, "WEEKLY", LocalDate.of(2026, 7, 15));

        assertThat(total.favoriteCount()).isEqualTo(7L);
        assertThat(weekly.favoriteCount()).isEqualTo(7L);
    }

    // 컨트롤러의 @Pattern 검증을 거치지 않는 서비스 직접 호출 경로에서 잘못된 periodType이
    // 조용히 TOTAL로 처리되지 않고 fail-fast하는지 확인한다(관리자 통계와 동일 정책).
    @Test
    void getStatistics_잘못된_periodType은_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> userStatisticService.getStatistics(MEMBER_ID, "DAILY", LocalDate.of(2026, 7, 15)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
