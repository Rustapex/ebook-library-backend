package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatus(String status);

    List<Challenge> findByStatusAndChallengeType(String status, String challengeType);

    List<Challenge> findByStatusAndTargetAction(String status, String targetAction);

    Page<Challenge> findByStatus(String status, Pageable pageable);

    Page<Challenge> findByChallengeType(String challengeType, Pageable pageable);

    Page<Challenge> findByStatusAndChallengeType(String status, String challengeType, Pageable pageable);
}
