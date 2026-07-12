package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.BookReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookReportRepository extends JpaRepository<BookReport, Long> {

    @Query("SELECT br FROM BookReport br JOIN FETCH br.reporter JOIN FETCH br.book ORDER BY br.createdAt DESC")
    List<BookReport> findRecent(Pageable pageable);
}
