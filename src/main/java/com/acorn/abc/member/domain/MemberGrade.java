package com.acorn.abc.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "member_grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @Column(name = "min_payment_amount", nullable = false)
    private int minPaymentAmount;

    @Column(name = "grade_period_months", nullable = false)
    private int gradePeriodMonths;

    @Column(name = "maintain_payment_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal maintainPaymentRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
