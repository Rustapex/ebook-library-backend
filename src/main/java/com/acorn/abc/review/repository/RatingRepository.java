package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByMember_MemberIdAndBook_BookId(Long memberId, Long bookId);

    int countByBook_BookId(Long bookId);

    List<Rating> findByBook_BookIdAndMember_MemberIdIn(Long bookId, Collection<Long> memberIds);
}
