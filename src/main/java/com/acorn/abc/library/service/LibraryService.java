package com.acorn.abc.library.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.library.dto.LibrarySummaryResponse;
import com.acorn.abc.library.repository.LibraryBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// open-in-view=false 환경에서 libraryBook.library(LAZY) 접근을 조회와 같은 세션에서 하기 위해 필요하다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryBookRepository libraryBookRepository;
    private final BookService bookService;

    public List<LibrarySummaryResponse> getLibrariesByBookId(Long bookId) {
        Book book = bookService.getAvailableBookOrThrow(bookId);

        return libraryBookRepository.findActiveByBookId(bookId).stream()
                .map(libraryBook -> new LibrarySummaryResponse(
                        book.getBookId(),
                        book.getTitle(),
                        book.getCoverImageUrl(),
                        libraryBook.getLibrary().getLibraryName(),
                        libraryBook.getLibrary().getAddress(),
                        libraryBook.getLibrary().getLatitude(),
                        libraryBook.getLibrary().getLongitude(),
                        libraryBook.getHoldingStatus()
                ))
                .toList();
    }
}
