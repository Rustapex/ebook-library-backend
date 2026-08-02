package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookEpub;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.book.service.BookEpubBinaryStore;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.EpubDownloadMetadata;
import com.acorn.abc.rental.dto.ProgressResponse;
import com.acorn.abc.rental.dto.ProgressUpdateRequest;
import com.acorn.abc.rental.dto.ViewerSessionResponse;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ViewerService {

    private final RentalService rentalService;
    private final BookEpubRepository bookEpubRepository;
    private final BookEpubBinaryStore bookEpubBinaryStore;
    private final ReadingProgressRepository readingProgressRepository;

    @Transactional(readOnly = true)
    public ViewerSessionResponse getSession(Long rentalId, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        Book book = rental.getBook();
        BookEpub epub = bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(book.getBookId()).orElse(null);
        if (epub == null) {
            return new ViewerSessionResponse(book.getTitle(), 0, false, null);
        }

        ViewerSessionResponse.ViewerProgressResponse response = readingProgressRepository
                .findByRental_RentalId(rentalId)
                .filter(progress -> progress.getEpubVersion() == epub.getEpubVersion())
                .map(this::toViewerProgress)
                .orElse(null);
        return new ViewerSessionResponse(book.getTitle(), epub.getEpubVersion(), true, response);
    }

    public EpubDownloadMetadata prepareDownload(Long rentalId, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalForUpdateOrThrow(rentalId, memberId);
        BookEpub epub = bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(rental.getBook().getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND));

        rental.startReadingIfNotStarted();
        readingProgressRepository.findByRental_RentalId(rentalId)
                .orElseGet(() -> readingProgressRepository.save(ReadingProgress.start(rental, epub.getEpubVersion())));

        return new EpubDownloadMetadata(epub.getBookId(), epub.getEpubVersion(), epub.getOriginalFileName(),
                epub.getFileSize(), epub.getSha256());
    }

    @Transactional(readOnly = true)
    public void writeEpub(EpubDownloadMetadata metadata, OutputStream output) {
        bookEpubBinaryStore.writeTo(metadata.bookId(), metadata.epubVersion(), output);
    }

    public ProgressResponse saveProgress(Long rentalId, ProgressUpdateRequest request, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalForUpdateOrThrow(rentalId, memberId);
        BookEpub epub = bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(rental.getBook().getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND));

        ReadingProgress progress = readingProgressRepository.findByRentalIdForUpdate(rentalId).orElse(null);
        if (request.epubVersion() != epub.getEpubVersion()) {
            throw new ProgressRevisionConflictException(progress == null ? 0 : progress.getRevision());
        }
        if (progress == null) {
            if (request.expectedRevision() != 0) {
                throw new ProgressRevisionConflictException(0);
            }
            progress = ReadingProgress.start(rental, epub.getEpubVersion());
        } else if (progress.getRevision() != request.expectedRevision()) {
            throw new ProgressRevisionConflictException(progress.getRevision());
        }

        BigDecimal progression = BigDecimal.valueOf(request.progression()).setScale(5, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        progress.updateLocation(epub.getEpubVersion(), request.cfi(), request.chapterHref(),
                request.spineIndex(), progression, now);
        progress.completeIfEpubEnd(request.atEnd(), progression, now);
        ReadingProgress saved = readingProgressRepository.saveAndFlush(progress);

        return new ProgressResponse(saved.getProgressId(), saved.getEpubVersion(), saved.getLocationCfi(),
                saved.getChapterHref(), saved.getSpineIndex(), saved.getProgression().doubleValue(),
                saved.getRevision(), Boolean.TRUE.equals(saved.getCompletedYn()));
    }

    private ViewerSessionResponse.ViewerProgressResponse toViewerProgress(ReadingProgress progress) {
        return new ViewerSessionResponse.ViewerProgressResponse(
                progress.getLocationCfi(), progress.getChapterHref(), progress.getSpineIndex(),
                progress.getProgression().doubleValue(), progress.getRevision(),
                Boolean.TRUE.equals(progress.getCompletedYn()));
    }
}
