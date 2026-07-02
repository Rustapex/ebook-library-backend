package com.acorn.abc.rental.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.Bookmark;
import com.acorn.abc.rental.dto.BookmarkCreateRequest;
import com.acorn.abc.rental.dto.BookmarkDeleteResponse;
import com.acorn.abc.rental.dto.BookmarkResponse;
import com.acorn.abc.rental.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final RentalService rentalService;
    private final BookmarkRepository bookmarkRepository;

    public BookmarkResponse addBookmark(Long rentalId, BookmarkCreateRequest request, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        if (bookmarkRepository.existsByRental_RentalIdAndPageNo(rentalId, request.pageNo())) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
        Bookmark bookmark = bookmarkRepository.save(Bookmark.create(rental, request.pageNo()));
        return new BookmarkResponse(bookmark.getBookmarkId(), bookmark.getPageNo(), bookmark.getCreatedAt());
    }

    public BookmarkDeleteResponse deleteBookmark(Long rentalId, Long bookmarkId, Long memberId) {
        rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        Bookmark bookmark = bookmarkRepository.findByBookmarkIdAndRental_RentalId(bookmarkId, rentalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
        return new BookmarkDeleteResponse(true);
    }

    public List<BookmarkResponse> getBookmarks(Long rentalId, Long memberId) {
        rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        return bookmarkRepository.findByRental_RentalIdOrderByPageNoAsc(rentalId).stream()
                .map(bookmark -> new BookmarkResponse(bookmark.getBookmarkId(), bookmark.getPageNo(), bookmark.getCreatedAt()))
                .toList();
    }
}
