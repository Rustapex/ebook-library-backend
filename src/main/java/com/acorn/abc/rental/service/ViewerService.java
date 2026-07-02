package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookPage;
import com.acorn.abc.book.repository.BookPageRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.ProgressResponse;
import com.acorn.abc.rental.dto.ProgressUpdateRequest;
import com.acorn.abc.rental.dto.ViewerPageResponse;
import com.acorn.abc.rental.repository.BookmarkRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ViewerService {

    private final RentalService rentalService;
    private final BookPageRepository bookPageRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;

    public ViewerPageResponse getPage(Long rentalId, int pageNo, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        Book book = rental.getBook();

        BookPage page = bookPageRepository.findByBook_BookIdAndPageNo(book.getBookId(), pageNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

        rental.startReadingIfNotStarted();
        ReadingProgress progress = readingProgressRepository.findByRental_RentalId(rentalId)
                .orElseGet(() -> readingProgressRepository.save(ReadingProgress.start(rental)));

        boolean bookmarkYn = bookmarkRepository.existsByRental_RentalIdAndPageNo(rentalId, pageNo);

        return new ViewerPageResponse(
                book.getTitle(),
                page.getPageContent(),
                book.getTotalPages(),
                progressRate(progress.getCurrentPage(), book.getTotalPages()),
                bookmarkYn
        );
    }

    public ProgressResponse saveProgress(Long rentalId, ProgressUpdateRequest request, Long memberId) {
        BookRental rental = rentalService.getOwnedRentalOrThrow(rentalId, memberId);
        Book book = rental.getBook();

        ReadingProgress progress = readingProgressRepository.findByRental_RentalId(rentalId)
                .orElseGet(() -> readingProgressRepository.save(ReadingProgress.start(rental)));

        LocalDateTime now = LocalDateTime.now();
        progress.moveToPage(request.currentPage(), now);
        progress.completeIfLastPage(book.getTotalPages(), now);

        return new ProgressResponse(
                progress.getProgressId(),
                progress.getCurrentPage(),
                progressRate(progress.getCurrentPage(), book.getTotalPages()),
                Boolean.TRUE.equals(progress.getCompletedYn())
        );
    }

    private double progressRate(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return 0;
        }
        return Math.min(100.0, currentPage * 100.0 / totalPages);
    }
}
