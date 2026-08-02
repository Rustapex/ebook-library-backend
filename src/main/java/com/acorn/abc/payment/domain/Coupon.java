package com.acorn.abc.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_name", nullable = false, length = 100)
    private String couponName;

    @Column(name = "coupon_type", nullable = false, length = 30)
    private String couponType;

    @Column(name = "benefit_value", nullable = false)
    private int benefitValue;

    @Column(name = "benefit_unit", nullable = false, length = 20)
    private String benefitUnit;

    @Column(name = "valid_days", nullable = false)
    private int validDays;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Coupon create(String couponName, String couponType, int benefitValue,
                                 String benefitUnit, int validDays, String status) {
        Coupon coupon = new Coupon();
        coupon.couponName = couponName;
        coupon.couponType = couponType;
        coupon.benefitValue = benefitValue;
        coupon.benefitUnit = benefitUnit;
        coupon.validDays = validDays;
        coupon.status = status;
        coupon.createdAt = LocalDateTime.now();
        return coupon;
    }
}
