package com.acorn.abc.statistic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_statistic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_stat_id")
    private Long adminStatId;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType;

    @Column(name = "age_band", nullable = false, length = 20)
    private String ageBand;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "total_member_count", nullable = false)
    private int totalMemberCount;

    @Column(name = "sanctioned_member_count", nullable = false)
    private int sanctionedMemberCount;

    @Column(name = "active_book_count", nullable = false)
    private int activeBookCount;

    @Column(name = "total_rental_count", nullable = false)
    private int totalRentalCount;

    @Column(name = "total_read_book_count", nullable = false)
    private int totalReadBookCount;

    @Column(name = "free_rental_count", nullable = false)
    private int freeRentalCount;

    @Column(name = "paid_rental_count", nullable = false)
    private int paidRentalCount;

    @Column(name = "total_payment_amount", nullable = false)
    private long totalPaymentAmount;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AdminStatistic create(String periodType, String ageBand,
                                         LocalDate periodStartDate, LocalDate periodEndDate) {
        AdminStatistic statistic = new AdminStatistic();
        statistic.periodType = periodType;
        statistic.ageBand = ageBand;
        statistic.periodStartDate = periodStartDate;
        statistic.periodEndDate = periodEndDate;
        // review_count/report_count는 review 패키지에 Repository가 없어(§4-4) 이번 배치 범위에서 0으로 고정한다.
        // 후속 작업(구현계획 §9)에서 review/report Repository가 추가되면 refreshCounts에 포함해 채운다.
        statistic.reviewCount = 0;
        statistic.reportCount = 0;
        statistic.updatedAt = LocalDateTime.now();
        return statistic;
    }

    public void refreshCounts(LocalDate periodEndDate,
                               int totalMemberCount,
                               int sanctionedMemberCount,
                               int activeBookCount,
                               int totalRentalCount,
                               int freeRentalCount,
                               int paidRentalCount,
                               int totalReadBookCount,
                               long totalPaymentAmount,
                               LocalDateTime updatedAt) {
        this.periodEndDate = periodEndDate;
        this.totalMemberCount = totalMemberCount;
        this.sanctionedMemberCount = sanctionedMemberCount;
        this.activeBookCount = activeBookCount;
        this.totalRentalCount = totalRentalCount;
        this.freeRentalCount = freeRentalCount;
        this.paidRentalCount = paidRentalCount;
        this.totalReadBookCount = totalReadBookCount;
        this.totalPaymentAmount = totalPaymentAmount;
        this.updatedAt = updatedAt;
    }
}
