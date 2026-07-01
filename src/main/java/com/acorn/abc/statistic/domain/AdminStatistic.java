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
}
