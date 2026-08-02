package com.acorn.abc.request.repository;

import com.acorn.abc.request.domain.BookRequestCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookRequestCandidateRepository extends JpaRepository<BookRequestCandidate, Long> {

    List<BookRequestCandidate> findAllByOrderByFirstRequestedAtDesc(Pageable pageable);

    // 정규화 3컬럼 UNIQUE(V002 마이그레이션 전제) 기준 원자적 upsert.
    // 후보가 이미 있으면 request_count만 증가시키고, 종료 상태(APPROVED/REJECTED)였으면 REQUESTED로 재오픈한다.
    @Modifying
    @Query(value = """
            INSERT INTO book_request_candidate
              (normalized_title, normalized_author, normalized_publisher, request_count, first_requested_at, candidate_status)
            VALUES (:normalizedTitle, :normalizedAuthor, :normalizedPublisher, 1, :now, 'REQUESTED')
            ON DUPLICATE KEY UPDATE
              request_candidate_id = LAST_INSERT_ID(request_candidate_id),
              request_count = request_count + 1,
              candidate_status = CASE WHEN candidate_status IN ('APPROVED','REJECTED') THEN 'REQUESTED' ELSE candidate_status END
            """, nativeQuery = true)
    void upsertCandidate(@Param("normalizedTitle") String normalizedTitle,
                          @Param("normalizedAuthor") String normalizedAuthor,
                          @Param("normalizedPublisher") String normalizedPublisher,
                          @Param("now") LocalDateTime now);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long lastInsertId();

    // REQUESTED -> IN_REVIEW. book_request row는 건드리지 않는다.
    @Modifying
    @Query("UPDATE BookRequestCandidate c SET c.candidateStatus = 'IN_REVIEW' " +
            "WHERE c.requestCandidateId = :candidateId AND c.candidateStatus = 'REQUESTED'")
    int moveToInReviewIfRequested(@Param("candidateId") Long candidateId);

    // REQUESTED/IN_REVIEW -> APPROVED/REJECTED.
    @Modifying
    @Query("UPDATE BookRequestCandidate c SET c.candidateStatus = :newStatus " +
            "WHERE c.requestCandidateId = :candidateId AND c.candidateStatus IN ('REQUESTED','IN_REVIEW')")
    int finalizeIfEligible(@Param("candidateId") Long candidateId, @Param("newStatus") String newStatus);

    // 관리자 후보 목록: latestRequestedAt(=MAX(book_request.created_at)) DESC, candidateId DESC 정렬.
    @Query(value = """
            SELECT c.* FROM book_request_candidate c
            LEFT JOIN (
              SELECT request_candidate_id, MAX(created_at) AS latest_requested_at
              FROM book_request GROUP BY request_candidate_id
            ) latest ON c.request_candidate_id = latest.request_candidate_id
            WHERE (:status IS NULL OR c.candidate_status = :status)
              AND (:q IS NULL OR LOWER(c.normalized_title) LIKE CONCAT('%', :q, '%'))
            ORDER BY latest.latest_requested_at DESC, c.request_candidate_id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM book_request_candidate c
            WHERE (:status IS NULL OR c.candidate_status = :status)
              AND (:q IS NULL OR LOWER(c.normalized_title) LIKE CONCAT('%', :q, '%'))
            """,
            nativeQuery = true)
    Page<BookRequestCandidate> search(@Param("status") String status, @Param("q") String q, Pageable pageable);
}
