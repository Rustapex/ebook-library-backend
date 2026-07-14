package com.acorn.abc.statistic.domain;

import com.acorn.abc.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 현재 미사용(dormant/legacy 후보) 엔티티다. API-STAT-001(GET /api/v1/me/statistics)은
 * 이 테이블 대신 book_rental/reading_progress/review/favorite_book을 실시간 집계한다
 * (회원용독서통계 구현계획 §4.1 확정, PR #46 리뷰 반영). 배치/스냅샷 통계가 필요해지면
 * 재활성화 또는 제거 여부를 별도로 검토한다.
 */
@Entity
@Table(name = "user_reading_statistic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserReadingStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_stat_id")
    private Long userStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "rental_count", nullable = false)
    private int rentalCount;

    @Column(name = "read_book_count", nullable = false)
    private int readBookCount;

    @Column(name = "read_page_count", nullable = false)
    private int readPageCount;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
