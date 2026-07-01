package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookAuthor;
import com.acorn.abc.book.domain.BookAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, BookAuthorId> {

    @Query("SELECT ba FROM BookAuthor ba JOIN FETCH ba.author a " +
            "WHERE ba.book.bookId = :bookId AND a.status = 'ACTIVE' " +
            "ORDER BY ba.authorOrder ASC, a.authorName ASC")
    List<BookAuthor> findActiveByBookId(@Param("bookId") Long bookId);

    @Query("SELECT ba FROM BookAuthor ba JOIN FETCH ba.author a " +
            "WHERE ba.book.bookId IN :bookIds AND a.status = 'ACTIVE' " +
            "ORDER BY ba.book.bookId ASC, ba.authorOrder ASC, a.authorName ASC")
    List<BookAuthor> findActiveByBookIds(@Param("bookIds") List<Long> bookIds);
}
