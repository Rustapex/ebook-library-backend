package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.BookReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookReportRepository extends JpaRepository<BookReport, Long> {

    @Query("SELECT br FROM BookReport br JOIN FETCH br.reporter JOIN FETCH br.book ORDER BY br.createdAt DESC")
    List<BookReport> findRecent(Pageable pageable);

    // 관리자 회원 상세 REPORTS 탭(구현계획 §1.3) — REVIEW_REPORT와 서비스에서 합쳐 정렬한다.
    @Query("SELECT br FROM BookReport br JOIN FETCH br.book WHERE br.reporter.memberId = :reporterId " +
            "ORDER BY br.createdAt DESC, br.bookReportId DESC")
    List<BookReport> findByReporterIdForAdmin(@Param("reporterId") Long reporterId);

    long countByReporter_MemberId(Long reporterId);

    // 관리자 통계 도서 신고 기간/나이대 조건부 집계(관리자 통계 확장 §3.1~§3.3, §6 — 나이대는 reporter 기준)
    @Query("SELECT COUNT(br) FROM BookReport br WHERE " +
            "(:from IS NULL OR br.createdAt >= :from) AND (:to IS NULL OR br.createdAt < :to) " +
            "AND (:birthDateFrom IS NULL OR br.reporter.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR br.reporter.birthDate <= :birthDateTo)")
    long countByStatisticConditions(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("birthDateFrom") LocalDate birthDateFrom,
                                     @Param("birthDateTo") LocalDate birthDateTo);
}
