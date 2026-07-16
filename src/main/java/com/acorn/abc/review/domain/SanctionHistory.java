package com.acorn.abc.review.domain;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "sanction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SanctionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_history_id")
    private Long sanctionHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_report_id")
    private ReviewReport reviewReport;

    @Column(name = "sanction_type", nullable = false, length = 30)
    private String sanctionType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 관리자 회원 API 구현계획(2026-07-16) §1.4 — 관리자가 직접 부여하는 제재(리뷰 신고 경유가 아닌 경우 reviewReport=null).
    public static SanctionHistory create(Member member, ReviewReport reviewReport, String sanctionType, String reason,
                                          LocalDateTime startedAt, LocalDateTime endedAt) {
        SanctionHistory sanctionHistory = new SanctionHistory();
        sanctionHistory.member = member;
        sanctionHistory.reviewReport = reviewReport;
        sanctionHistory.sanctionType = sanctionType;
        sanctionHistory.reason = reason;
        sanctionHistory.startedAt = startedAt;
        sanctionHistory.endedAt = endedAt;
        sanctionHistory.createdAt = LocalDateTime.now();
        return sanctionHistory;
    }
}
