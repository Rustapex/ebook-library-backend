package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookDetailRepository extends JpaRepository<BookDetail, Long> {

    Optional<BookDetail> findByBook_BookId(Long bookId);
}
