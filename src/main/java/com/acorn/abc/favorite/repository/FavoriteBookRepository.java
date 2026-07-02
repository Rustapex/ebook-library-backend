package com.acorn.abc.favorite.repository;

import com.acorn.abc.favorite.domain.FavoriteBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteBookRepository extends JpaRepository<FavoriteBook, Long> {

    boolean existsByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    @Query("SELECT fb.book.bookId FROM FavoriteBook fb " +
            "WHERE fb.member.memberId = :memberId AND fb.book.bookId IN :bookIds")
    List<Long> findBookIdsByMemberIdAndBookIdIn(@Param("memberId") Long memberId, @Param("bookIds") List<Long> bookIds);
}
