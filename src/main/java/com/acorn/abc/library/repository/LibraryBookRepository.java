package com.acorn.abc.library.repository;

import com.acorn.abc.library.domain.LibraryBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    @Query("SELECT lb FROM LibraryBook lb JOIN FETCH lb.library l " +
            "WHERE lb.book.bookId = :bookId AND l.status = 'ACTIVE' " +
            "ORDER BY l.libraryName ASC")
    List<LibraryBook> findActiveByBookId(@Param("bookId") Long bookId);
}
