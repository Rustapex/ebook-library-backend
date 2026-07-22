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
@Table(name = "review_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_report_id")
    private Long reviewReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "process_result", columnDefinition = "TEXT")
    private String processResult;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 관리자 신고 처리(API-ADMIN-REPORT-002, 2026-07-22 구현계획 §2.4) — 상태 전이는 서비스에서 검증한 뒤 호출한다.
    public void process(String status, String processResult, LocalDateTime processedAt) {
        this.status = status;
        this.processResult = processResult;
        this.processedAt = processedAt;
    }
}
