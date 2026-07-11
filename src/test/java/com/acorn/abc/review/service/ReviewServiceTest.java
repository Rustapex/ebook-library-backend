package com.acorn.abc.review.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private BookService bookService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BookRentalRepository bookRentalRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Member member;
    private Book book;

    // 성공 케이스에서만 실제로 쓰이는 getter라 조기 예외 발생 테스트에서는 unused stub이 될 수 있어 lenient로 둔다
    private void stubMemberAndBook() {
        member = mock(Member.class);
        org.mockito.Mockito.lenient().when(member.getMemberId()).thenReturn(1L);
        org.mockito.Mockito.lenient().when(member.getName()).thenReturn("테스터");
        org.mockito.Mockito.lenient().when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        book = mock(Book.class);
        org.mockito.Mockito.lenient().when(book.getBookId()).thenReturn(10L);
        given(bookService.getAvailableBookOrThrow(10L)).willReturn(book);
    }

    private void stubReadingStarted() {
        BookRental rental = mock(BookRental.class);
        given(rental.getRentalStartAt()).willReturn(LocalDateTime.now());
        given(bookRentalRepository.findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(1L, 10L))
                .willReturn(Optional.of(rental));
    }

    @Test
    void getBookReviews_지원하지_않는_sort이면_VALIDATION_ERROR를_던진다() {
        stubMemberAndBook();

        assertThatThrownBy(() -> reviewService.getBookReviews(10L, null, null, "popular"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(reviewRepository, never()).findActiveByBookId(any(), any());
    }

    @Test
    void getBookReviews_sort이_없으면_latest_기본값으로_정상조회된다() {
        stubMemberAndBook();
        given(book.getAverageRating()).willReturn(BigDecimal.valueOf(4.5));
        given(book.getReviewCount()).willReturn(2);
        given(reviewRepository.findActiveByBookId(eq(10L), any())).willReturn(new PageImpl<>(List.of()));
        given(ratingRepository.findByBook_BookIdAndMember_MemberIdIn(eq(10L), any())).willReturn(List.of());

        ReviewListResponse response = reviewService.getBookReviews(10L, null, null, null);

        assertThat(response.summary().reviewCount()).isEqualTo(2);
        assertThat(response.summary().averageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void createOrRestoreReview_읽기를_시작하지_않았으면_REVIEW_NOT_ALLOWED를_던진다() {
        stubMemberAndBook();
        given(bookRentalRepository.findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(1L, 10L))
                .willReturn(Optional.empty());

        ReviewRequest request = new ReviewRequest(10L, 5, "좋아요");

        assertThatThrownBy(() -> reviewService.createOrRestoreReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createOrRestoreReview_READY상태로_rentalStartAt이_null이면_REVIEW_NOT_ALLOWED를_던진다() {
        stubMemberAndBook();
        BookRental rental = mock(BookRental.class);
        given(rental.getRentalStartAt()).willReturn(null);
        given(bookRentalRepository.findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(1L, 10L))
                .willReturn(Optional.of(rental));

        ReviewRequest request = new ReviewRequest(10L, 5, "좋아요");

        assertThatThrownBy(() -> reviewService.createOrRestoreReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    void createOrRestoreReview_기존리뷰가_없으면_리뷰와_별점을_새로_생성한다() {
        stubMemberAndBook();
        stubReadingStarted();
        given(reviewRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.empty());
        given(ratingRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.empty());
        given(ratingRepository.countByBook_BookId(10L)).willReturn(0);

        ReviewRequest request = new ReviewRequest(10L, 5, "좋아요");
        ReviewResponse response = reviewService.createOrRestoreReview(1L, request);

        assertThat(response.content()).isEqualTo("좋아요");
        assertThat(response.ratingScore()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(Review.STATUS_ACTIVE);
        verify(book).increaseReviewCount();
        verify(book).applyNewRating(0, 5);
        verify(ratingRepository).save(any(Rating.class));
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createOrRestoreReview_기존리뷰가_ACTIVE이면_REVIEW_ALREADY_EXISTS를_던진다() {
        stubMemberAndBook();
        stubReadingStarted();
        Review existing = mock(Review.class);
        given(existing.isActive()).willReturn(true);
        given(reviewRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.of(existing));

        ReviewRequest request = new ReviewRequest(10L, 5, "좋아요");

        assertThatThrownBy(() -> reviewService.createOrRestoreReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void createOrRestoreReview_기존리뷰가_HIDDEN이면_AUTH_ACCESS_DENIED를_던진다() {
        stubMemberAndBook();
        stubReadingStarted();
        Review existing = mock(Review.class);
        given(existing.isActive()).willReturn(false);
        given(existing.isHidden()).willReturn(true);
        given(reviewRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.of(existing));

        ReviewRequest request = new ReviewRequest(10L, 5, "좋아요");

        assertThatThrownBy(() -> reviewService.createOrRestoreReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(existing, never()).restore(any());
    }

    @Test
    void createOrRestoreReview_기존리뷰가_DELETED이고_기존Rating이_있으면_리뷰만_복구하고_평균은_변경하지_않는다() {
        stubMemberAndBook();
        stubReadingStarted();
        Review existing = mock(Review.class);
        given(existing.isActive()).willReturn(false);
        given(existing.isHidden()).willReturn(false);
        given(existing.getContent()).willReturn("복구된 내용");
        given(existing.getStatus()).willReturn(Review.STATUS_ACTIVE);
        given(existing.getBook()).willReturn(book);
        given(existing.getMember()).willReturn(member);
        given(reviewRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.of(existing));

        Rating existingRating = mock(Rating.class);
        given(existingRating.getRatingId()).willReturn(99L);
        given(existingRating.getRatingScore()).willReturn((byte) 4);
        given(ratingRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.of(existingRating));

        // 클라이언트가 다른 점수(2)를 보내도 기존 Rating 값(4)이 그대로 응답에 반환되어야 한다 (A안)
        ReviewRequest request = new ReviewRequest(10L, 2, "복구된 내용");
        ReviewResponse response = reviewService.createOrRestoreReview(1L, request);

        assertThat(response.ratingScore()).isEqualTo(4);
        verify(existing).restore("복구된 내용");
        verify(book).increaseReviewCount();
        verify(book, never()).applyNewRating(any(Integer.class), any(Integer.class));
        verify(ratingRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_소유자가_아니면_AUTH_ACCESS_DENIED를_던진다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(2L);
        given(review.getMember()).willReturn(owner);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(1L, 5L, new ReviewUpdateRequest("수정")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void updateReview_HIDDEN이면_AUTH_ACCESS_DENIED를_던진다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(true);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(1L, 5L, new ReviewUpdateRequest("수정")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(review, never()).updateContent(any());
        verify(review, never()).restore(any());
    }

    @Test
    void updateReview_ACTIVE면_내용만_수정하고_reviewCount는_변경하지_않는다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(false);
        given(review.isDeleted()).willReturn(false);
        Book targetBook = mock(Book.class);
        given(targetBook.getBookId()).willReturn(10L);
        given(review.getBook()).willReturn(targetBook);
        given(review.getStatus()).willReturn(Review.STATUS_ACTIVE);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));
        given(ratingRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.empty());

        ReviewResponse response = reviewService.updateReview(1L, 5L, new ReviewUpdateRequest("수정된 내용"));

        verify(review).updateContent("수정된 내용");
        verify(review, never()).restore(any());
        verify(targetBook, never()).increaseReviewCount();
        assertThat(response.status()).isEqualTo(Review.STATUS_ACTIVE);
    }

    @Test
    void updateReview_DELETED면_ACTIVE로_복구하고_reviewCount를_증가시킨다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(false);
        given(review.isDeleted()).willReturn(true);
        Book targetBook = mock(Book.class);
        given(targetBook.getBookId()).willReturn(10L);
        given(review.getBook()).willReturn(targetBook);
        given(review.getStatus()).willReturn(Review.STATUS_ACTIVE);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));
        given(ratingRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.empty());

        reviewService.updateReview(1L, 5L, new ReviewUpdateRequest("복구"));

        verify(review).restore("복구");
        verify(targetBook).increaseReviewCount();
    }

    @Test
    void deleteReview_소유자가_아니면_AUTH_ACCESS_DENIED를_던진다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(2L);
        given(review.getMember()).willReturn(owner);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void deleteReview_HIDDEN이면_AUTH_ACCESS_DENIED를_던진다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(true);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(review, never()).softDelete();
    }

    @Test
    void deleteReview_ACTIVE면_DELETED로_변경하고_reviewCount를_감소시킨다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(false);
        given(review.isActive()).willReturn(true);
        given(review.getStatus()).willReturn(Review.STATUS_DELETED);
        Book targetBook = mock(Book.class);
        given(review.getBook()).willReturn(targetBook);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        ReviewDeleteResponse response = reviewService.deleteReview(1L, 5L);

        verify(review).softDelete();
        verify(targetBook).decreaseReviewCount();
        verify(targetBook, never()).applyNewRating(any(Integer.class), any(Integer.class));
        assertThat(response.status()).isEqualTo(Review.STATUS_DELETED);
    }

    @Test
    void deleteReview_이미_DELETED면_멱등하게_처리하고_reviewCount를_다시_감소시키지_않는다() {
        Review review = mock(Review.class);
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(1L);
        given(review.getMember()).willReturn(owner);
        given(review.isHidden()).willReturn(false);
        given(review.isActive()).willReturn(false);
        given(review.getStatus()).willReturn(Review.STATUS_DELETED);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        reviewService.deleteReview(1L, 5L);

        verify(review, never()).softDelete();
    }
}
