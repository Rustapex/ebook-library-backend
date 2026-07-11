package com.acorn.abc.review.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.dto.BookReviewSummaryResponse;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.review.domain.Rating;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.dto.ReviewDeleteResponse;
import com.acorn.abc.review.dto.ReviewListResponse;
import com.acorn.abc.review.dto.ReviewRequest;
import com.acorn.abc.review.dto.ReviewResponse;
import com.acorn.abc.review.dto.ReviewUpdateRequest;
import com.acorn.abc.review.repository.RatingRepository;
import com.acorn.abc.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final BookService bookService;
    private final MemberRepository memberRepository;
    private final BookRentalRepository bookRentalRepository;
    private final ReviewRepository reviewRepository;
    private final RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public ReviewListResponse getBookReviews(Long bookId, Integer page, Integer size, String sort) {
        Book book = getBookOrThrow(bookId);
        normalizeSort(sort);

        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("reviewId")));
        Page<Review> reviews = reviewRepository.findActiveByBookId(bookId, pageable);

        List<Long> memberIds = reviews.getContent().stream().map(review -> review.getMember().getMemberId()).toList();
        Map<Long, Byte> ratingScoreByMemberId = ratingRepository.findByBook_BookIdAndMember_MemberIdIn(bookId, memberIds).stream()
                .collect(java.util.stream.Collectors.toMap(rating -> rating.getMember().getMemberId(), Rating::getRatingScore));

        List<ReviewResponse> content = reviews.getContent().stream()
                .map(review -> toReviewResponse(review, null, ratingScoreByMemberId.get(review.getMember().getMemberId())))
                .toList();

        BookReviewSummaryResponse summary = new BookReviewSummaryResponse(defaultBigDecimal(book.getAverageRating()), book.getReviewCount());
        PageResponse<ReviewResponse> pageResponse = PageResponse.of(content, reviews.getNumber(), reviews.getSize(), reviews.getTotalElements());
        return new ReviewListResponse(summary, pageResponse);
    }

    // 상태 처리 규칙: 기존 Review 없음 -> 신규 생성 / ACTIVE -> 409 / DELETED -> 같은 row 복구(Rating 재사용) / HIDDEN -> 403
    public ReviewResponse createOrRestoreReview(Long memberId, ReviewRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Book book = getBookOrThrow(request.bookId());

        assertReadingStarted(memberId, request.bookId());

        Review existingReview = reviewRepository.findByMember_MemberIdAndBook_BookId(memberId, request.bookId()).orElse(null);

        if (existingReview != null && existingReview.isActive()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        if (existingReview != null && existingReview.isHidden()) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        Rating existingRating = ratingRepository.findByMember_MemberIdAndBook_BookId(memberId, request.bookId()).orElse(null);
        Rating rating = existingRating != null ? existingRating : createRating(book, member, request.ratingScore());

        Review review;
        if (existingReview != null) {
            // DELETED -> ACTIVE 복구. ratingScore 요청값은 검증만 하고(A안) 기존 Rating을 그대로 사용한다.
            existingReview.restore(request.content());
            review = existingReview;
        } else {
            review = Review.create(member, book, request.content());
            reviewRepository.save(review);
        }
        book.increaseReviewCount();

        return toReviewResponse(review, rating.getRatingId(), rating.getRatingScore());
    }

    public ReviewResponse updateReview(Long memberId, Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        assertOwner(review, memberId);

        if (review.isHidden()) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        boolean wasDeleted = review.isDeleted();
        if (wasDeleted) {
            review.restore(request.content());
            review.getBook().increaseReviewCount();
        } else {
            review.updateContent(request.content());
        }

        Rating rating = ratingRepository.findByMember_MemberIdAndBook_BookId(memberId, review.getBook().getBookId()).orElse(null);
        Byte ratingScore = rating == null ? null : rating.getRatingScore();
        Long ratingId = rating == null ? null : rating.getRatingId();
        return toReviewResponse(review, ratingId, ratingScore);
    }

    public ReviewDeleteResponse deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        assertOwner(review, memberId);

        if (review.isHidden()) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        // ACTIVE -> DELETED만 허용. 이미 DELETED면 멱등하게 현재 상태만 반환한다 (averageRating은 항상 불변)
        if (review.isActive()) {
            review.softDelete();
            review.getBook().decreaseReviewCount();
        }

        return new ReviewDeleteResponse(review.getReviewId(), review.getStatus());
    }

    private Rating createRating(Book book, Member member, Integer ratingScore) {
        int existingRatingCount = ratingRepository.countByBook_BookId(book.getBookId());
        Rating rating = Rating.create(member, book, ratingScore);
        ratingRepository.save(rating);
        book.applyNewRating(existingRatingCount, ratingScore);
        return rating;
    }

    private void assertReadingStarted(Long memberId, Long bookId) {
        BookRental rental = bookRentalRepository
                .findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(memberId, bookId)
                .orElse(null);
        if (rental == null || rental.getRentalStartAt() == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
    }

    private void assertOwner(Review review, Long memberId) {
        if (!review.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    // User API 공통 규칙: BOOK.status=AVAILABLE만 노출한다 (BookService에 위치를 통일)
    private Book getBookOrThrow(Long bookId) {
        return bookService.getAvailableBookOrThrow(bookId);
    }

    private ReviewResponse toReviewResponse(Review review, Long ratingId, Byte ratingScore) {
        return new ReviewResponse(
                review.getReviewId(),
                ratingId,
                review.getBook().getBookId(),
                review.getMember().getMemberId(),
                review.getMember().getName(),
                ratingScore == null ? null : ratingScore.intValue(),
                review.getContent(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 현재 지원 sort는 latest뿐이다. 값이 없으면 기본값(latest)로 취급하고, 그 외 값은 VALIDATION_ERROR로 응답한다
    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "latest";
        }
        if (!"latest".equals(sort.trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return "latest";
    }
}
