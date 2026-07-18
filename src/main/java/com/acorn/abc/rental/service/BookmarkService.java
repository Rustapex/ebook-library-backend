package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.BookEpub;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.Bookmark;
import com.acorn.abc.rental.dto.BookmarkCreateRequest;
import com.acorn.abc.rental.dto.BookmarkDeleteResponse;
import com.acorn.abc.rental.dto.BookmarkResponse;
import com.acorn.abc.rental.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final RentalService rentalService;
    private final BookmarkRepository bookmarkRepository;
    private final BookEpubRepository bookEpubRepository;

    public BookmarkResponse addBookmark(Long rentalId, BookmarkCreateRequest request, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        BookEpub epub = bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(rental.getBook().getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND));
        if (request.epubVersion() != epub.getEpubVersion()) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
        byte[] hash = sha256(request.cfi());
        if (bookmarkRepository.existsByRental_RentalIdAndEpubVersionAndLocationHash(
                rentalId, request.epubVersion(), hash)) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
        try {
            Bookmark bookmark = bookmarkRepository.saveAndFlush(Bookmark.create(
                    rental, request.epubVersion(), request.cfi(), hash, request.chapterHref(),
                    request.spineIndex(), progression(request.progression())));
            return toResponse(bookmark);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
    }

    public BookmarkDeleteResponse deleteBookmark(Long rentalId, Long bookmarkId, Long memberId) {
        rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        Bookmark bookmark = bookmarkRepository.findByBookmarkIdAndRental_RentalId(bookmarkId, rentalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
        return new BookmarkDeleteResponse(true);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(Long rentalId, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        BookEpub epub = bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(rental.getBook().getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND));
        return bookmarkRepository.findByRental_RentalIdAndEpubVersionOrderByProgressionAsc(
                        rentalId, epub.getEpubVersion()).stream()
                .map(this::toResponse)
                .toList();
    }

    private BookmarkResponse toResponse(Bookmark bookmark) {
        return new BookmarkResponse(bookmark.getBookmarkId(), bookmark.getEpubVersion(),
                bookmark.getLocationCfi(), bookmark.getChapterHref(), bookmark.getSpineIndex(),
                bookmark.getProgression().doubleValue(), bookmark.getCreatedAt());
    }

    private BigDecimal progression(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
