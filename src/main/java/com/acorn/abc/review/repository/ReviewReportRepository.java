package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.ReviewReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    @Query("SELECT rr FROM ReviewReport rr JOIN FETCH rr.reporter JOIN FETCH rr.review rv JOIN FETCH rv.book ORDER BY rr.createdAt DESC")
    List<ReviewReport> findRecent(Pageable pageable);
}
