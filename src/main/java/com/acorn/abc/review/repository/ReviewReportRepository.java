package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.ReviewReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    @Query("SELECT rr FROM ReviewReport rr JOIN FETCH rr.reporter JOIN FETCH rr.review rv JOIN FETCH rv.book ORDER BY rr.createdAt DESC")
    List<ReviewReport> findRecent(Pageable pageable);

    // 관리자 신고 처리(API-ADMIN-REPORT-002, PR64 코드리뷰 대응 §2.2) — 동일 신고 중복 처리를 막기 위한 잠금 조회.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM ReviewReport rr WHERE rr.reviewReportId = :reviewReportId")
    Optional<ReviewReport> findByIdForUpdate(@Param("reviewReportId") Long reviewReportId);

    long countByReporter_MemberId(Long reporterId);

    // 관리자 통계 리뷰 신고 기간/나이대 조건부 집계(관리자 통계 확장 §3.1~§3.3, §6 — 나이대는 reporter 기준)
    @Query("SELECT COUNT(rr) FROM ReviewReport rr WHERE " +
            "(:from IS NULL OR rr.createdAt >= :from) AND (:to IS NULL OR rr.createdAt < :to) " +
            "AND (:birthDateFrom IS NULL OR rr.reporter.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR rr.reporter.birthDate <= :birthDateTo)")
    long countByStatisticConditions(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("birthDateFrom") LocalDate birthDateFrom,
                                     @Param("birthDateTo") LocalDate birthDateTo);
}
