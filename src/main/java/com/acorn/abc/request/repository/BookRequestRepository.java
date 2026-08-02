package com.acorn.abc.request.repository;

import com.acorn.abc.admin.request.dto.AdminBookRequestApplicantResponse;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.request.domain.BookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {

    // 관리자 승인/반려: 현재 회차(request_status='REQUESTED')인 row만 벌크로 반영한다.
    @Modifying
    @Query("UPDATE BookRequest br SET br.requestStatus = :status, br.approvedBook = :approvedBook, " +
            "br.rejectReason = :rejectReason, br.processedAt = :now " +
            "WHERE br.requestCandidate.requestCandidateId = :candidateId AND br.requestStatus = 'REQUESTED'")
    int updateStatusForRequestedByCandidateId(@Param("candidateId") Long candidateId,
                                               @Param("status") String status,
                                               @Param("approvedBook") Book approvedBook,
                                               @Param("rejectReason") String rejectReason,
                                               @Param("now") LocalDateTime now);

    long countByRequestCandidate_RequestCandidateIdAndRequestStatus(Long candidateId, String requestStatus);

    // 빠른 실패용 사전 확인. uk_book_request_member_normalized UNIQUE 제약이 동시성 최종 방어선이다.
    boolean existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
            Long memberId, String normalizedTitle, String normalizedAuthor, String normalizedPublisher);

    // 내 신청 내역: status는 조인 계산값 기준으로 필터링한다(상태 모델 참고).
    @Query("SELECT br FROM BookRequest br JOIN FETCH br.requestCandidate c " +
            "WHERE br.member.memberId = :memberId " +
            "AND (:status IS NULL " +
            "  OR (:status = 'REQUESTED' AND br.requestStatus = 'REQUESTED' AND c.candidateStatus = 'REQUESTED') " +
            "  OR (:status = 'IN_REVIEW' AND br.requestStatus = 'REQUESTED' AND c.candidateStatus = 'IN_REVIEW') " +
            "  OR (:status = 'APPROVED' AND br.requestStatus = 'APPROVED') " +
            "  OR (:status = 'REJECTED' AND br.requestStatus = 'REJECTED')) " +
            "ORDER BY br.createdAt DESC, br.requestId DESC")
    Page<BookRequest> findMyRequests(@Param("memberId") Long memberId, @Param("status") String status, Pageable pageable);

    @Query("SELECT new com.acorn.abc.admin.request.dto.AdminBookRequestApplicantResponse(" +
            "m.memberId, m.loginId, m.name, br.reason, br.createdAt) " +
            "FROM BookRequest br JOIN br.member m " +
            "WHERE br.requestCandidate.requestCandidateId = :candidateId " +
            "ORDER BY br.createdAt DESC, br.requestId DESC")
    List<AdminBookRequestApplicantResponse> findApplicantsByCandidateId(@Param("candidateId") Long candidateId);

    // Candidate 응답 파생 규칙: candidate_status=requestStatus인 row 중 processedAt DESC, requestId DESC 최신 1건.
    Optional<BookRequest> findFirstByRequestCandidate_RequestCandidateIdAndRequestStatusOrderByProcessedAtDescRequestIdDesc(
            Long candidateId, String requestStatus);

    // 관리자 후보 목록의 대표 title/author/publisher(원문 표기) 표시용 — 최초 신청 row 기준.
    Optional<BookRequest> findFirstByRequestCandidate_RequestCandidateIdOrderByCreatedAtAsc(Long candidateId);

    // 관리자 승인/반려 벌크 UPDATE 전에 알림 대상(이번 회차 REQUESTED 회원)을 미리 조회해 둔다.
    @Query("SELECT br FROM BookRequest br JOIN FETCH br.member " +
            "WHERE br.requestCandidate.requestCandidateId = :candidateId AND br.requestStatus = 'REQUESTED'")
    List<BookRequest> findRequestedByCandidateId(@Param("candidateId") Long candidateId);
}
