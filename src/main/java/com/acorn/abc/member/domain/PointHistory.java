package com.acorn.abc.member.domain;

import com.acorn.abc.payment.domain.Payment;
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
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long pointHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "point_type", nullable = false, length = 30)
    private String pointType;

    @Column(name = "point_amount", nullable = false)
    private int pointAmount;

    @Column(name = "description", length = 255)
    private String description;

    // 관리자 회원 API 구현계획(2026-07-16) §1.6 — 관리자 포인트 조정(ADMIN_ADJUST)의 회원별 멱등 키.
    // 기존 이력은 backfill 없이 null로 유지되며, ADMIN_ADJUST만 서비스에서 non-null을 강제한다.
    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PointHistory create(Member member, Payment payment, String pointType, int pointAmount, String description) {
        PointHistory pointHistory = new PointHistory();
        pointHistory.member = member;
        pointHistory.payment = payment;
        pointHistory.pointType = pointType;
        pointHistory.pointAmount = pointAmount;
        pointHistory.description = description;
        pointHistory.createdAt = LocalDateTime.now();
        return pointHistory;
    }

    public static PointHistory createAdminAdjust(Member member, int pointAmount, String description, String requestId) {
        PointHistory pointHistory = new PointHistory();
        pointHistory.member = member;
        pointHistory.pointType = "ADMIN_ADJUST";
        pointHistory.pointAmount = pointAmount;
        pointHistory.description = description;
        pointHistory.requestId = requestId;
        pointHistory.createdAt = LocalDateTime.now();
        return pointHistory;
    }

    public static PointHistory createPaymentUse(Member member, Payment payment, int pointUsed, LocalDateTime createdAt) {
        PointHistory history = new PointHistory();
        history.member = member;
        history.payment = payment;
        history.pointType = "PAYMENT_USE";
        history.pointAmount = -pointUsed;
        history.createdAt = createdAt;
        return history;
    }
}
