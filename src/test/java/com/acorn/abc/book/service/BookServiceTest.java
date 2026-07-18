package com.acorn.abc.book.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.dto.BookDetailResponse;
import com.acorn.abc.book.repository.BookAuthorRepository;
import com.acorn.abc.book.repository.BookCategoryRepository;
import com.acorn.abc.book.repository.BookDetailRepository;
import com.acorn.abc.book.repository.BookKeywordMapRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.category.service.CategoryService;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.repository.BookRentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// PR #21 리뷰 코멘트(favoriteYn/myRentalState 반영) 후속 조치로 추가한 최소 커버 테스트.
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookAuthorRepository bookAuthorRepository;
    @Mock
    private BookCategoryRepository bookCategoryRepository;
    @Mock
    private BookKeywordMapRepository bookKeywordMapRepository;
    @Mock
    private BookDetailRepository bookDetailRepository;
    @Mock
    private FavoriteBookRepository favoriteBookRepository;
    @Mock
    private BookRentalRepository bookRentalRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private BookEpubRepository bookEpubRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = mock(Book.class);
        // parentCategoryId 관련 테스트는 빈 결과라 book을 아예 안 건드려서 unused stub이 될 수 있어 lenient로 둔다
        org.mockito.Mockito.lenient().when(book.getBookId()).thenReturn(1L);
    }

    @Test
    void getBooks_비로그인이면_모든카드의_favoriteYn이_false다() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1));
        given(bookAuthorRepository.findActiveByBookIds(anyList())).willReturn(List.of());

        PageResponse<BookCardResponse> result = bookService.getBooks(
                null, null, null, null, null, null, null, null, null);

        assertThat(result.content().get(0).favoriteYn()).isFalse();
        verify(favoriteBookRepository, never()).findBookIdsByMemberIdAndBookIdIn(any(), any());
    }

    @Test
    void getBooks_로그인회원이_즐겨찾기한_도서면_favoriteYn이_true다() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1));
        given(bookAuthorRepository.findActiveByBookIds(anyList())).willReturn(List.of());
        given(favoriteBookRepository.findBookIdsByMemberIdAndBookIdIn(eq(10L), anyList()))
                .willReturn(List.of(1L));

        PageResponse<BookCardResponse> result = bookService.getBooks(
                null, null, null, null, null, null, null, null, 10L);

        assertThat(result.content().get(0).favoriteYn()).isTrue();
    }

    @Test
    void getBooks_parentCategoryId가_있으면_CategoryService로_하위카테고리를_조회한다() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        given(categoryService.findDescendantIds(158L)).willReturn(List.of(158L, 159L, 160L));

        bookService.getBooks(null, 158L, null, null, null, null, null, null, null);

        verify(categoryService).findDescendantIds(158L);
    }

    @Test
    void getBooks_parentCategoryId가_없으면_CategoryService를_호출하지_않는다() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        bookService.getBooks(null, null, null, null, null, null, null, null, null);

        verify(categoryService, never()).findDescendantIds(any());
    }

    @Test
    void getBookDetail_비로그인이면_myRentalState는_null이고_myFavoriteYn은_false다() {
        given(book.getStatus()).willReturn("AVAILABLE");
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookDetailRepository.findByBook_BookId(1L)).willReturn(Optional.empty());
        given(bookAuthorRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookCategoryRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookKeywordMapRepository.findActiveByBookId(1L)).willReturn(List.of());

        BookDetailResponse result = bookService.getBookDetail(1L, null);

        assertThat(result.myRentalState()).isNull();
        assertThat(result.myFavoriteYn()).isFalse();
        verify(bookRentalRepository, never())
                .findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(any(), any());
        verify(favoriteBookRepository, never()).existsByMember_MemberIdAndBook_BookId(any(), any());
    }

    @Test
    void getBookDetail_로그인회원이_즐겨찾기했으면_myFavoriteYn이_true다() {
        given(book.getStatus()).willReturn("AVAILABLE");
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookDetailRepository.findByBook_BookId(1L)).willReturn(Optional.empty());
        given(bookAuthorRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookCategoryRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookKeywordMapRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookRentalRepository.findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(10L, 1L))
                .willReturn(Optional.empty());
        given(favoriteBookRepository.existsByMember_MemberIdAndBook_BookId(10L, 1L)).willReturn(true);

        BookDetailResponse result = bookService.getBookDetail(1L, 10L);

        assertThat(result.myFavoriteYn()).isTrue();
    }

    @Test
    void getBookDetail_최근대여기록이있으면_myRentalState에_최신상태가_반영된다() {
        given(book.getStatus()).willReturn("AVAILABLE");
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookDetailRepository.findByBook_BookId(1L)).willReturn(Optional.empty());
        given(bookAuthorRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookCategoryRepository.findActiveByBookId(1L)).willReturn(List.of());
        given(bookKeywordMapRepository.findActiveByBookId(1L)).willReturn(List.of());
        BookRental latestRental = mock(BookRental.class);
        given(latestRental.getRentalStatus()).willReturn("RENTED");
        given(bookRentalRepository.findFirstByMember_MemberIdAndBook_BookIdOrderByCreatedAtDescRentalIdDesc(10L, 1L))
                .willReturn(Optional.of(latestRental));
        given(favoriteBookRepository.existsByMember_MemberIdAndBook_BookId(10L, 1L)).willReturn(false);

        BookDetailResponse result = bookService.getBookDetail(1L, 10L);

        assertThat(result.myRentalState()).isEqualTo("RENTED");
    }
}
