package com.acorn.abc.favorite.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.favorite.domain.FavoriteBook;
import com.acorn.abc.favorite.dto.FavoriteDeleteResponse;
import com.acorn.abc.favorite.dto.FavoriteRequest;
import com.acorn.abc.favorite.dto.FavoriteResponse;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

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
class FavoriteServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FavoriteBookRepository favoriteBookRepository;
    @Mock
    private BookService bookService;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void addFavorite_정상요청이면_즐겨찾기를_생성한다() {
        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(10L);
        given(bookService.getAvailableBookOrThrow(10L)).willReturn(book);
        given(favoriteBookRepository.existsByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(false);

        FavoriteBook saved = FavoriteBook.create(member, book);
        given(favoriteBookRepository.saveAndFlush(any(FavoriteBook.class))).willReturn(saved);

        FavoriteResponse response = favoriteService.addFavorite(1L, new FavoriteRequest(10L));

        assertThat(response.bookId()).isEqualTo(10L);
    }

    @Test
    void addFavorite_이미_등록된_즐겨찾기면_FAVORITE_ALREADY_EXISTS를_던진다() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(bookService.getAvailableBookOrThrow(10L)).willReturn(mock(Book.class));
        given(favoriteBookRepository.existsByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(1L, new FavoriteRequest(10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FAVORITE_ALREADY_EXISTS);
        verify(favoriteBookRepository, never()).saveAndFlush(any());
    }

    @Test
    void addFavorite_도서가_없으면_BOOK_NOT_FOUND를_던진다() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(bookService.getAvailableBookOrThrow(10L)).willThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        assertThatThrownBy(() -> favoriteService.addFavorite(1L, new FavoriteRequest(10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void removeFavorite_존재하지_않으면_FAVORITE_NOT_FOUND를_던진다() {
        given(favoriteBookRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FAVORITE_NOT_FOUND);
    }

    @Test
    void removeFavorite_존재하면_삭제하고_deleted를_true로_반환한다() {
        FavoriteBook favoriteBook = mock(FavoriteBook.class);
        given(favoriteBookRepository.findByMember_MemberIdAndBook_BookId(1L, 10L)).willReturn(Optional.of(favoriteBook));

        FavoriteDeleteResponse response = favoriteService.removeFavorite(1L, 10L);

        assertThat(response.deleted()).isTrue();
        verify(favoriteBookRepository).delete(favoriteBook);
    }

    @Test
    void getMyFavorites_지원하지_않는_sort이면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> favoriteService.getMyFavorites(1L, null, null, "popular"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(favoriteBookRepository, never()).findMyFavorites(any(), any());
    }

    @Test
    void getMyFavorites_sort이_없으면_latest_기본값으로_정상조회된다() {
        FavoriteBook favoriteBook = mock(FavoriteBook.class);
        Book book = mock(Book.class);
        given(favoriteBook.getBook()).willReturn(book);
        given(favoriteBookRepository.findMyFavorites(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(favoriteBook)));
        given(bookService.toCardResponses(any(), eq(1L))).willReturn(List.<BookCardResponse>of());

        PageResponse<BookCardResponse> response = favoriteService.getMyFavorites(1L, null, null, null);

        assertThat(response.totalElements()).isEqualTo(1);
    }
}
