package com.acorn.abc.challenge.domain;

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

@Entity
@Table(name = "challenge_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_progress_id")
    private Long challengeProgressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Column(name = "achieved_yn", nullable = false)
    private Boolean achievedYn;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @Column(name = "reward_claimed_at")
    private LocalDateTime rewardClaimedAt;

    public static ChallengeProgress create(Member member, Challenge challenge, LocalDate progressDate) {
        ChallengeProgress progress = new ChallengeProgress();
        progress.member = member;
        progress.challenge = challenge;
        progress.progressDate = progressDate;
        progress.currentCount = 0;
        progress.achievedYn = false;
        return progress;
    }

    public void incrementProgress(int targetCount) {
        if (Boolean.TRUE.equals(this.achievedYn)) {
            return;
        }
        this.currentCount++;
        if (this.currentCount >= targetCount) {
            this.achievedYn = true;
            this.achievedAt = LocalDateTime.now();
        }
    }

    public void markRewardClaimed() {
        this.rewardClaimedAt = LocalDateTime.now();
    }
}
