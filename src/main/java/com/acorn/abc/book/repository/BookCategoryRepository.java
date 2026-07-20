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

    // 관리자 도서 목록(API-ADMIN-BOOK-001) 전용 — 공개 조회의 category.status='ACTIVE' 필터 없이 전부 반환한다
    // (관리자 도서 API 설계문서 §5, 공개 findActiveByBookId는 재사용하지 않음).
    @Query("SELECT bc FROM BookCategory bc JOIN FETCH bc.category c " +
            "WHERE bc.book.bookId IN :bookIds " +
            "ORDER BY bc.book.bookId ASC, c.displayOrder ASC, c.categoryId ASC")
    List<BookCategory> findAllByBookIdIn(@Param("bookIds") List<Long> bookIds);

    // 관리자 도서 수정(API-ADMIN-BOOK-003) 전용 — 기존 category 매핑 전체 교체용 삭제.
    void deleteByBook_BookId(Long bookId);
}
