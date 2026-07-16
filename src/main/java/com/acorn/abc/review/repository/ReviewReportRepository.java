package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.ReviewReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    @Query("SELECT rr FROM ReviewReport rr JOIN FETCH rr.reporter JOIN FETCH rr.review rv JOIN FETCH rv.book ORDER BY rr.createdAt DESC")
    List<ReviewReport> findRecent(Pageable pageable);

    // 관리자 회원 상세 REPORTS 탭(구현계획 §1.3) — BOOK_REPORT와 서비스에서 합쳐 정렬한다.
    @Query("SELECT rr FROM ReviewReport rr JOIN FETCH rr.review v WHERE rr.reporter.memberId = :reporterId " +
            "ORDER BY rr.createdAt DESC, rr.reviewReportId DESC")
    List<ReviewReport> findByReporterIdForAdmin(@Param("reporterId") Long reporterId);

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
