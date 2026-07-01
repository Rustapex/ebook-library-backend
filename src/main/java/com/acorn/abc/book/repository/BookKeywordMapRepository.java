package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookKeywordMap;
import com.acorn.abc.book.domain.BookKeywordMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookKeywordMapRepository extends JpaRepository<BookKeywordMap, BookKeywordMapId> {

    @Query("SELECT bkm FROM BookKeywordMap bkm JOIN FETCH bkm.keyword k " +
            "WHERE bkm.book.bookId = :bookId AND k.status = 'ACTIVE' " +
            "ORDER BY bkm.displayOrder ASC, k.keywordName ASC")
    List<BookKeywordMap> findActiveByBookId(@Param("bookId") Long bookId);
}
