package com.acorn.abc.admin.report.repository;

import com.acorn.abc.review.domain.BookReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 관리자 신고 목록 조회(API-ADMIN-REPORT-001, 2026-07-22 구현계획 §2.4) 전용 조회 repository.
 * book_report와 review_report를 UNION ALL로 함께 조회하므로 어느 한 Entity에도 종속되지 않는다
 * (admin.member.repository.AdminMemberReportRepository와 동일한 이유로 Repository&lt;BookReport, Long&gt;에 타입 바인딩).
 * 모달 표시용 신고자·대상·신고 내용을 이 쿼리 안에서 함께 조립해 별도 상세 API 없이 응답한다.
 */
public interface AdminReportRepository extends Repository<BookReport, Long> {

    @Query(value = "SELECT report_id AS reportId, target_type AS targetType, reporter_id AS reporterId, "
            + "reporter_name AS reporterName, book_id AS bookId, book_title AS bookTitle, "
            + "review_id AS reviewId, review_content AS reviewContent, report_type AS reportType, "
            + "content AS content, status AS status, process_result AS processResult, "
            + "processed_at AS processedAt, created_at AS createdAt FROM ("
            + "  SELECT br.book_report_id AS report_id, 'BOOK' AS target_type, br.reporter_id AS reporter_id, "
            + "         m.name AS reporter_name, br.book_id AS book_id, b.title AS book_title, "
            + "         CAST(NULL AS UNSIGNED) AS review_id, CAST(NULL AS CHAR) AS review_content, "
            + "         br.report_type AS report_type, br.content AS content, br.status AS status, "
            + "         br.process_result AS process_result, br.processed_at AS processed_at, br.created_at AS created_at "
            + "  FROM book_report br JOIN member m ON br.reporter_id = m.member_id JOIN book b ON br.book_id = b.book_id "
            + "  UNION ALL "
            + "  SELECT rr.review_report_id AS report_id, 'REVIEW' AS target_type, rr.reporter_id AS reporter_id, "
            + "         m2.name AS reporter_name, b2.book_id AS book_id, b2.title AS book_title, "
            + "         rv.review_id AS review_id, rv.content AS review_content, "
            + "         rr.report_type AS report_type, rr.content AS content, rr.status AS status, "
            + "         rr.process_result AS process_result, rr.processed_at AS processed_at, rr.created_at AS created_at "
            + "  FROM review_report rr JOIN member m2 ON rr.reporter_id = m2.member_id "
            + "       JOIN review rv ON rr.review_id = rv.review_id JOIN book b2 ON rv.book_id = b2.book_id "
            + ") merged "
            + "WHERE (:targetType IS NULL OR target_type = :targetType) AND (:status IS NULL OR status = :status) "
            + "ORDER BY created_at DESC, report_id DESC",
            countQuery = "SELECT COUNT(*) FROM ("
            + "  SELECT br.book_report_id AS report_id, 'BOOK' AS target_type, br.status AS status "
            + "  FROM book_report br "
            + "  UNION ALL "
            + "  SELECT rr.review_report_id AS report_id, 'REVIEW' AS target_type, rr.status AS status "
            + "  FROM review_report rr "
            + ") merged WHERE (:targetType IS NULL OR target_type = :targetType) AND (:status IS NULL OR status = :status)",
            nativeQuery = true)
    Page<AdminReportRow> search(@Param("targetType") String targetType, @Param("status") String status, Pageable pageable);

    interface AdminReportRow {
        Long getReportId();

        String getTargetType();

        Long getReporterId();

        String getReporterName();

        Long getBookId();

        String getBookTitle();

        Long getReviewId();

        String getReviewContent();

        String getReportType();

        String getContent();

        String getStatus();

        String getProcessResult();

        LocalDateTime getProcessedAt();

        LocalDateTime getCreatedAt();
    }
}
