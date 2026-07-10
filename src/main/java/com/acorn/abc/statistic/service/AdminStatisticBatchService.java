package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.statistic.domain.AdminStatistic;
import com.acorn.abc.statistic.repository.AdminStatisticRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * admin_statistic의 TOTAL/ALL(전체 기간·전체 나이대) 통계만 채우는 1단계 배치(구현계획 §4-1).
 * WEEKLY/MONTHLY/YEARLY와 나이대별 세부 집계는 이번 범위가 아니며 §9(남은 작업)에서 다룬다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatisticBatchService {

    // TOTAL/ALL 통계의 period_start_date 고정값. 실제 서비스 시작일이 아니라
    // UNIQUE(period_type, period_start_date, age_band) 제약에서 항상 같은 행을 가리키게 하기 위한 상수다.
    static final LocalDate TOTAL_PERIOD_START_DATE = LocalDate.of(2000, 1, 1);
    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    private static final String AGE_BAND_ALL = "ALL";

    private final AdminStatisticRepository adminStatisticRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final PaymentRepository paymentRepository;

    // 배포 직후 스케줄 실행 전에도 API가 TOTAL/ALL 행을 곧바로 조회할 수 있도록 앱 기동 시 1회 실행한다.
    @PostConstruct
    @Transactional
    public void refreshOnStartup() {
        refreshTotalStatistics();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void refreshTotalStatistics() {
        LocalDate today = LocalDate.now();

        long totalMemberCount = memberRepository.count();
        long sanctionedMemberCount = memberRepository.countByStatus("SANCTIONED");
        long activeBookCount = bookRepository.countByStatus("AVAILABLE");
        long totalRentalCount = bookRentalRepository.count();
        long freeRentalCount = bookRentalRepository.countByBook_RentalType("FREE");
        long paidRentalCount = bookRentalRepository.countByBook_RentalType("PAID");
        long totalReadBookCount = readingProgressRepository.countByCompletedYn(true);
        long totalPaymentAmount = paymentRepository.sumAmountByPaymentStatus("PAID");

        AdminStatistic statistic = adminStatisticRepository
                .findByPeriodTypeAndPeriodStartDateAndAgeBand(PERIOD_TYPE_TOTAL, TOTAL_PERIOD_START_DATE, AGE_BAND_ALL)
                .orElseGet(() -> AdminStatistic.create(PERIOD_TYPE_TOTAL, AGE_BAND_ALL, TOTAL_PERIOD_START_DATE, today));

        statistic.refreshCounts(
                today,
                (int) totalMemberCount,
                (int) sanctionedMemberCount,
                (int) activeBookCount,
                (int) totalRentalCount,
                (int) freeRentalCount,
                (int) paidRentalCount,
                (int) totalReadBookCount,
                totalPaymentAmount,
                LocalDateTime.now());

        adminStatisticRepository.save(statistic);
        log.info("admin_statistic TOTAL/ALL 통계 갱신 완료 (totalMemberCount={}, totalRentalCount={})",
                totalMemberCount, totalRentalCount);
    }
}
