package com.acorn.abc.library.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.library.dto.LibrarySummaryResponse;
import com.acorn.abc.library.repository.LibraryBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final LibraryBookRepository libraryBookRepository;
    private final BookService bookService;

    public List<LibrarySummaryResponse> getLibrariesByBookId(Long bookId) {
        Book book = bookService.getAvailableBookOrThrow(bookId);

        return libraryBookRepository.findActiveByBookId(bookId).stream()
                .map(libraryBook -> new LibrarySummaryResponse(
                        book.getBookId(),
                        book.getTitle(),
                        libraryBook.getLibrary().getLibraryName(),
                        libraryBook.getLibrary().getAddress(),
                        libraryBook.getLibrary().getLatitude(),
                        libraryBook.getLibrary().getLongitude(),
                        libraryBook.getHoldingStatus()
                ))
                .toList();
    }
}
