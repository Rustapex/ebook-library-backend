package com.acorn.abc.favorite.repository;

import com.acorn.abc.favorite.domain.FavoriteBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteBookRepository extends JpaRepository<FavoriteBook, Long> {

    boolean existsByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    Optional<FavoriteBook> findByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    @Query("SELECT fb.book.bookId FROM FavoriteBook fb " +
            "WHERE fb.member.memberId = :memberId AND fb.book.bookId IN :bookIds")
    List<Long> findBookIdsByMemberIdAndBookIdIn(@Param("memberId") Long memberId, @Param("bookIds") List<Long> bookIds);

    // 목록 조회는 User 공개 API 규칙(BOOK.status=AVAILABLE만 노출)을 따른다.
    // FAVORITE_BOOK row 자체는 삭제하지 않으므로 삭제(findByMember_MemberIdAndBook_BookId)는 상태와 무관하게 그대로 둔다.
    @Query(value = "SELECT fb FROM FavoriteBook fb JOIN FETCH fb.book " +
            "WHERE fb.member.memberId = :memberId AND fb.book.status = 'AVAILABLE' " +
            "ORDER BY fb.createdAt DESC, fb.favoriteId DESC",
            countQuery = "SELECT COUNT(fb) FROM FavoriteBook fb " +
            "WHERE fb.member.memberId = :memberId AND fb.book.status = 'AVAILABLE'")
    Page<FavoriteBook> findMyFavorites(@Param("memberId") Long memberId, Pageable pageable);
}
