package com.acorn.abc.member.domain;

import com.acorn.abc.common.domain.BaseEntity;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.review.domain.SanctionHistory;
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

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private MemberGrade grade;

    @Column(name = "grade_expires_at")
    private LocalDateTime gradeExpiresAt;

    @Column(name = "grade_payment_started_at")
    private LocalDateTime gradePaymentStartedAt;

    @Column(name = "point_balance", nullable = false)
    private int pointBalance;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    @Column(name = "withdrawal_reason", length = 255)
    private String withdrawalReason;

    // 관리자 회원 API 구현계획(2026-07-16) §1.5 — 현재 SANCTIONED 상태를 만든 계정 정지 이력.
    // ACCOUNT_SUSPENSION에서만 설정되며, 만료 배치가 이 값으로 원인을 검증한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_sanction_id")
    private SanctionHistory currentSanction;

    // TODO: 기본 등급 정책 확정 전까지 grade_id=1 사용
    public static Member create(String loginId, String passwordHash, String name, String email,
                                String phone, LocalDate birthDate, String gender, MemberGrade grade) {
        Member member = new Member();
        member.loginId = loginId;
        member.passwordHash = passwordHash;
        member.name = name;
        member.email = email;
        member.phone = phone;
        member.birthDate = birthDate;
        member.gender = gender;
        member.grade = grade;
        member.role = "USER";
        member.status = "JOINED";
        member.pointBalance = 0;
        return member;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String name, String email, String phone, String gender) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
    }

    public void applyPointChange(int amount) {
        this.pointBalance += amount;
    }

    public void usePoint(int amount) {
        if (amount > this.pointBalance) {
            throw new BusinessException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }
        this.pointBalance -= amount;
    }

    // 관리자 회원 API 구현계획 §1.6.2 — 조정값(+/-) 적용 후 잔액이 음수가 되면 거부한다.
    public void applyAdminPointAdjustment(int amount) {
        if (this.pointBalance + amount < 0) {
            throw new BusinessException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }
        this.pointBalance += amount;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    // 관리자 회원 API 구현계획 §1.1-4 — WITHDRAWN/DEACTIVATED 대상은 상태·제재·포인트 변경을 거부한다.
    public boolean isProtectedFromAdminChanges() {
        return isAdmin() || "WITHDRAWN".equals(this.status) || "DEACTIVATED".equals(this.status);
    }

    // 관리자 회원 API 구현계획 §1.4-1 — ACCOUNT_SUSPENSION만 상태를 SANCTIONED로 바꾸고 원인 제재를 연결한다.
    public void applyAccountSuspension(SanctionHistory sanction) {
        this.status = "SANCTIONED";
        this.currentSanction = sanction;
    }

    // 관리자가 직접 상태를 지정하는 경로(제재 없이 status만 변경). SANCTIONED가 아닌 상태로 바뀌면
    // 이전 계정 정지 원인 연결값은 더 이상 유효하지 않으므로 함께 비운다.
    public void changeStatusDirectly(String status) {
        this.status = status;
        if (!"SANCTIONED".equals(status)) {
            this.currentSanction = null;
        }
    }

    // 만료 배치 전용 — 실제 복구(§1.5-7)
    public void restoreFromSuspension() {
        this.status = "JOINED";
        this.currentSanction = null;
    }

    // 만료 배치 전용 — 다른 활성 계정 정지로 원인 재지정(§1.5-6)
    public void reassignCurrentSanction(SanctionHistory sanction) {
        this.currentSanction = sanction;
    }
}
