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

    // 관리자 도서 목록(API-ADMIN-BOOK-001) 전용 — 공개 조회의 author.status='ACTIVE' 필터 없이 전부 반환한다
    // (관리자 도서 API 설계문서 §5, 공개 findActiveByBookIds는 재사용하지 않음).
    @Query("SELECT ba FROM BookAuthor ba JOIN FETCH ba.author a " +
            "WHERE ba.book.bookId IN :bookIds " +
            "ORDER BY ba.book.bookId ASC, ba.authorOrder ASC, a.authorName ASC")
    List<BookAuthor> findAllByBookIdIn(@Param("bookIds") List<Long> bookIds);

    // 관리자 도서 상세(API-ADMIN-BOOK-005) 전용 — status 무관 전체 반환, authorOrder ASC/authorId ASC 고정 정렬
    // (관리자 도서 API PR52 코드리뷰 대응계획 §2.1).
    @Query("SELECT ba FROM BookAuthor ba JOIN FETCH ba.author a " +
            "WHERE ba.book.bookId = :bookId " +
            "ORDER BY ba.authorOrder ASC, a.authorId ASC")
    List<BookAuthor> findForAdminDetail(@Param("bookId") Long bookId);

    // 관리자 도서 수정(API-ADMIN-BOOK-003) 전용 — 기존 author 매핑 전체 교체용 삭제.
    void deleteByBook_BookId(Long bookId);
}
