package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.book.domain.BookCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookCategoryRepository extends JpaRepository<BookCategory, BookCategoryId> {

    @Query("SELECT bc FROM BookCategory bc JOIN FETCH bc.category c " +
            "WHERE bc.book.bookId = :bookId AND c.status = 'ACTIVE' " +
            "ORDER BY c.displayOrder ASC, c.categoryId ASC")
    List<BookCategory> findActiveByBookId(@Param("bookId") Long bookId);
}
