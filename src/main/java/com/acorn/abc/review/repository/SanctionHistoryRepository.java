package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.SanctionHistory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SanctionHistoryRepository extends JpaRepository<SanctionHistory, Long> {

    // 관리자 회원 상세 SANCTIONS 탭 — startedAt DESC, sanctionHistoryId DESC 고정(구현계획 §1.3).
    @Query(value = "SELECT s FROM SanctionHistory s WHERE s.member.memberId = :memberId " +
            "ORDER BY s.startedAt DESC, s.sanctionHistoryId DESC",
            countQuery = "SELECT COUNT(s) FROM SanctionHistory s WHERE s.member.memberId = :memberId")
    Page<SanctionHistory> findByMemberIdForAdmin(@Param("memberId") Long memberId, Pageable pageable);

    // 만료 배치(§1.5-3) — 회원 행과 함께 관련 제재 행도 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SanctionHistory s WHERE s.sanctionHistoryId = :sanctionHistoryId")
    Optional<SanctionHistory> findByIdForUpdate(@Param("sanctionHistoryId") Long sanctionHistoryId);

    // 만료 배치 후보(§1.5-2) — ACCOUNT_SUSPENSION이고 endedAt < now, ADMIN 대상은 후보 단계에서 제외.
    @Query("SELECT s FROM SanctionHistory s WHERE s.sanctionType = 'ACCOUNT_SUSPENSION' " +
            "AND s.endedAt IS NOT NULL AND s.endedAt < :now AND s.member.role <> 'ADMIN' " +
            "ORDER BY s.endedAt ASC")
    List<SanctionHistory> findExpiredAccountSuspensionCandidates(@Param("now") LocalDateTime now);

    // 만료 배치(§1.5-6) — 같은 회원의 다른 활성 계정 정지를 최신순으로 조회(재지정 대상 판단용).
    @Query("SELECT s FROM SanctionHistory s WHERE s.member.memberId = :memberId AND s.sanctionType = 'ACCOUNT_SUSPENSION' " +
            "AND (s.endedAt IS NULL OR s.endedAt >= :now) ORDER BY s.startedAt DESC, s.sanctionHistoryId DESC")
    List<SanctionHistory> findActiveAccountSuspensionsByMember(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // 회원 목록(API-ADMIN-MEMBER-001) — 페이지 내 여러 회원의 "현재 유효 제재"를 한 번에 조회(N+1 방지).
    @Query("SELECT s.member.memberId AS memberId, s.sanctionType AS sanctionType, s.endedAt AS endedAt " +
            "FROM SanctionHistory s WHERE s.member.memberId IN :memberIds " +
            "AND s.startedAt <= :now AND (s.endedAt IS NULL OR s.endedAt >= :now)")
    List<ActiveSanctionSummaryView> findActiveSanctionSummariesByMemberIds(@Param("memberIds") List<Long> memberIds,
                                                                            @Param("now") LocalDateTime now);

    interface ActiveSanctionSummaryView {
        Long getMemberId();
        String getSanctionType();
        LocalDateTime getEndedAt();
    }
}
