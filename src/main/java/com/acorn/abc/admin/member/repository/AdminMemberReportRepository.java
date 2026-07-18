package com.acorn.abc.admin.member.repository;

import com.acorn.abc.review.domain.BookReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// 관리자 회원 상세 REPORTS 탭(PR51 코드리뷰 대응 §2.3-3) 전용 조회 전용 repository.
// book_report와 review_report를 UNION ALL로 함께 조회하므로 어느 한 Entity에도 종속되지 않는다.
// Spring Data JPA는 Repository<Object, Long>의 Object를 managed entity로 해석하지 못해 컨텍스트 로딩에
// 실패하므로(Hibernate JpaMetamodelImpl), CRUD에는 쓰지 않지만 타입 바인딩용으로 실제 Entity(BookReport)를 사용한다.
public interface AdminMemberReportRepository extends Repository<BookReport, Long> {

    // 서비스 메모리에서 두 테이블 전체 목록을 합쳐 정렬·페이지를 자르지 않도록,
    // UNION ALL 결과 자체에 정렬과 LIMIT/OFFSET(Pageable)을 적용한다.
    @Query(value = "SELECT report_id AS reportId, report_source_type AS reportSourceType, report_type AS reportType, "
            + "status AS status, processed_at AS processedAt, created_at AS createdAt FROM ("
            + "  SELECT book_report_id AS report_id, 'BOOK_REPORT' AS report_source_type, report_type, status, processed_at, created_at "
            + "  FROM book_report WHERE reporter_id = :reporterId "
            + "  UNION ALL "
            + "  SELECT review_report_id AS report_id, 'REVIEW_REPORT' AS report_source_type, report_type, status, processed_at, created_at "
            + "  FROM review_report WHERE reporter_id = :reporterId "
            + ") merged ORDER BY created_at DESC, report_id DESC",
            countQuery = "SELECT COUNT(*) FROM ("
            + "  SELECT book_report_id FROM book_report WHERE reporter_id = :reporterId "
            + "  UNION ALL "
            + "  SELECT review_report_id FROM review_report WHERE reporter_id = :reporterId "
            + ") merged",
            nativeQuery = true)
    Page<AdminMemberReportRow> findByReporterId(@Param("reporterId") Long reporterId, Pageable pageable);

    interface AdminMemberReportRow {
        Long getReportId();

        String getReportSourceType();

        String getReportType();

        String getStatus();

        LocalDateTime getProcessedAt();

        LocalDateTime getCreatedAt();
    }
}
