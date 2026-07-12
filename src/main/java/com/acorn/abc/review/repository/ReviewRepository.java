package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    long countByStatus(String status);

    // 관리자 통계 리뷰 기간/나이대 조건부 집계(관리자 통계 확장 §3.1~§3.3). 나이대는 작성자(member) 기준.
    @Query("SELECT COUNT(r) FROM Review r WHERE r.status = :status " +
            "AND (:from IS NULL OR r.createdAt >= :from) AND (:to IS NULL OR r.createdAt < :to) " +
            "AND (:birthDateFrom IS NULL OR r.member.birthDate > :birthDateFrom) " +
            "AND (:birthDateTo IS NULL OR r.member.birthDate <= :birthDateTo)")
    long countByStatisticConditions(@Param("status") String status,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("birthDateFrom") LocalDate birthDateFrom,
                                     @Param("birthDateTo") LocalDate birthDateTo);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.member " +
            "WHERE r.book.bookId = :bookId AND r.status = 'ACTIVE' " +
            "ORDER BY r.createdAt DESC, r.reviewId DESC",
            countQuery = "SELECT COUNT(r) FROM Review r " +
            "WHERE r.book.bookId = :bookId AND r.status = 'ACTIVE'")
    Page<Review> findActiveByBookId(@Param("bookId") Long bookId, Pageable pageable);
}
