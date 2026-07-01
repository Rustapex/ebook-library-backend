package com.acorn.abc.challenge.domain;

import com.acorn.abc.payment.domain.Coupon;
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

@Entity
@Table(name = "challenge_reward")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_reward_id")
    private Long challengeRewardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(name = "reward_type", nullable = false, length = 20)
    private String rewardType;

    @Column(name = "reward_point")
    private Integer rewardPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "reward_quantity", nullable = false)
    private int rewardQuantity;
}
