package com.acorn.abc.admin.book.service;

import com.acorn.abc.admin.book.dto.EpubUploadResponse;
import com.acorn.abc.book.domain.BookEpub;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.service.BookEpubBinaryStore;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.repository.BookmarkRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminBookEpubService {

    private final BookRepository bookRepository;
    private final BookEpubRepository bookEpubRepository;
    private final BookEpubBinaryStore bookEpubBinaryStore;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;
    private final EpubFileValidator epubFileValidator;

    @Transactional
    public EpubUploadResponse upload(Long bookId, MultipartFile file) {
        bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        EpubValidationResult validated = epubFileValidator.validate(file);

        int epubVersion = bookEpubBinaryStore.insertNextVersion(bookId, validated.safeFileName(),
                validated.fileSize(), validated.sha256(), file);
        BookEpub saved = bookEpubRepository.findByBook_BookIdAndEpubVersion(bookId, epubVersion)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND));

        readingProgressRepository.resetForEpubVersion(bookId, saved.getEpubVersion());
        bookmarkRepository.deleteByRental_Book_BookId(bookId);

        return new EpubUploadResponse(saved.getBookId(), saved.getOriginalFileName(), saved.getFileSize(),
                saved.getSha256(), saved.getEpubVersion());
    }
}
