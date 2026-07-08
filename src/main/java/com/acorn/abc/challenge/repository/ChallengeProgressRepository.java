package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.ChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeProgressRepository extends JpaRepository<ChallengeProgress, Long> {

    Optional<ChallengeProgress> findByMember_MemberIdAndChallenge_ChallengeIdAndProgressDate(
            Long memberId, Long challengeId, LocalDate progressDate);

    Optional<ChallengeProgress> findFirstByMember_MemberIdAndChallenge_ChallengeId(Long memberId, Long challengeId);

    /** DAILY 챌린지 목록 조회용 batch 진행도 조회(N+1 방지). */
    List<ChallengeProgress> findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
            Long memberId, List<Long> challengeIds, LocalDate progressDate);

    /** TOTAL 챌린지 목록 조회용 batch 진행도 조회(N+1 방지). */
    List<ChallengeProgress> findByMember_MemberIdAndChallenge_ChallengeIdIn(
            Long memberId, List<Long> challengeIds);

    /** 동시 보상 수령 요청 시 중복 지급을 막기 위한 원자적 선점 업데이트. 영향받은 row 수(0 또는 1)로 선점 성공 여부를 판단한다. */
    @Modifying
    @Query("UPDATE ChallengeProgress cp SET cp.rewardClaimedAt = :claimedAt "
            + "WHERE cp.challengeProgressId = :challengeProgressId AND cp.rewardClaimedAt IS NULL")
    int markRewardClaimedIfUnclaimed(@Param("challengeProgressId") Long challengeProgressId,
                                      @Param("claimedAt") LocalDateTime claimedAt);
}
