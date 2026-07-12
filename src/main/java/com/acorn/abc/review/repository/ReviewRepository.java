package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    long countByStatus(String status);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.member " +
            "WHERE r.book.bookId = :bookId AND r.status = 'ACTIVE' " +
            "ORDER BY r.createdAt DESC, r.reviewId DESC",
            countQuery = "SELECT COUNT(r) FROM Review r " +
            "WHERE r.book.bookId = :bookId AND r.status = 'ACTIVE'")
    Page<Review> findActiveByBookId(@Param("bookId") Long bookId, Pageable pageable);
}
