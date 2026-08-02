package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.ChallengeReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChallengeRewardRepository extends JpaRepository<ChallengeReward, Long> {

    List<ChallengeReward> findByChallenge_ChallengeId(Long challengeId);

    List<ChallengeReward> findByChallenge_ChallengeIdIn(Collection<Long> challengeIds);
}
