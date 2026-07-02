package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.BookRental;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRentalRepository extends JpaRepository<BookRental, Long> {

    // book_rental은 (member_id, book_id) UNIQUE 제약이 없어 재대여 시 행이 여러 개 쌓일 수 있다 -> 최신 1건만 조회
    Optional<BookRental> findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDesc(Long memberId, Long bookId);
}
