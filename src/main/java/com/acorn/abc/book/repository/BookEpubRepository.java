package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookEpub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookEpubRepository extends JpaRepository<BookEpub, Long> {

    Optional<BookEpub> findFirstByBook_BookIdOrderByEpubVersionDesc(Long bookId);

    Optional<BookEpub> findByBook_BookIdAndEpubVersion(Long bookId, int epubVersion);

    boolean existsByBook_BookId(Long bookId);

    @Query("SELECT DISTINCT be.book.bookId FROM BookEpub be WHERE be.book.bookId IN :bookIds")
    List<Long> findBookIdsByBookIdIn(@Param("bookIds") Collection<Long> bookIds);
}
