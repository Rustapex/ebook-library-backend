package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.ChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ChallengeProgressRepository extends JpaRepository<ChallengeProgress, Long> {

    Optional<ChallengeProgress> findByMember_MemberIdAndChallenge_ChallengeIdAndProgressDate(
            Long memberId, Long challengeId, LocalDate progressDate);

    Optional<ChallengeProgress> findFirstByMember_MemberIdAndChallenge_ChallengeId(Long memberId, Long challengeId);
}
