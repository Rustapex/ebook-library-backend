package com.acorn.abc.challenge.domain;

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
@Table(name = "challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(name = "challenge_name", nullable = false, length = 100)
    private String challengeName;

    @Column(name = "challenge_type", nullable = false, length = 20)
    private String challengeType;

    @Column(name = "target_action", nullable = false, length = 50)
    private String targetAction;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void update(String challengeName, String challengeType, String targetAction, int targetCount, String status) {
        this.challengeName = challengeName;
        this.challengeType = challengeType;
        this.targetAction = targetAction;
        this.targetCount = targetCount;
        this.status = status;
    }
}
