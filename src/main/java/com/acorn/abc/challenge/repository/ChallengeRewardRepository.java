package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.ChallengeReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRewardRepository extends JpaRepository<ChallengeReward, Long> {

    List<ChallengeReward> findByChallenge_ChallengeId(Long challengeId);
}
