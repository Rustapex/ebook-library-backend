package com.acorn.abc.library.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.library.domain.Library;
import com.acorn.abc.library.domain.LibraryBook;
import com.acorn.abc.library.dto.LibrarySummaryResponse;
import com.acorn.abc.library.repository.LibraryBookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// PR #21 리뷰 코멘트(LibrarySummaryResponse.coverImageUrl 추가) 후속 조치로 추가한 최소 커버 테스트.
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryBookRepository libraryBookRepository;
    @Mock
    private BookService bookService;

    @InjectMocks
    private LibraryService libraryService;

    @Test
    void getLibrariesByBookId_응답에_coverImageUrl이_포함된다() {
        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(1L);
        given(book.getTitle()).willReturn("Effective Java");
        given(book.getCoverImageUrl()).willReturn("http://cover.example/1.jpg");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        Library library = mock(Library.class);
        given(library.getLibraryName()).willReturn("강남 도서관");
        given(library.getAddress()).willReturn("서울시 강남구");

        LibraryBook libraryBook = mock(LibraryBook.class);
        given(libraryBook.getLibrary()).willReturn(library);
        given(libraryBook.getHoldingStatus()).willReturn("AVAILABLE");
        given(libraryBookRepository.findActiveByBookId(1L)).willReturn(List.of(libraryBook));

        List<LibrarySummaryResponse> result = libraryService.getLibrariesByBookId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).coverImageUrl()).isEqualTo("http://cover.example/1.jpg");
        assertThat(result.get(0).libraryName()).isEqualTo("강남 도서관");
        assertThat(result.get(0).holdingStatus()).isEqualTo("AVAILABLE");
    }
}
