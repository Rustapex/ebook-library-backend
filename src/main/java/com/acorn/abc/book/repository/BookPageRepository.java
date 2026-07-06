package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookPageRepository extends JpaRepository<BookPage, Long> {

    Optional<BookPage> findByBook_BookIdAndPageNo(Long bookId, int pageNo);

    long countByBook_BookId(Long bookId);
}
