package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.RecentBookResponse;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final ReadingProgressRepository readingProgressRepository;

    public List<RecentBookResponse> getRecentBooks(Long memberId, Integer limit) {
        Pageable pageable = PageRequest.of(0, normalizeLimit(limit));
        return readingProgressRepository.findRecentByMemberId(memberId, pageable).stream()
                .map(this::toResponse)
                .toList();
    }

    private RecentBookResponse toResponse(ReadingProgress progress) {
        BookRental rental = progress.getRental();
        Book book = rental.getBook();
        return new RecentBookResponse(
                rental.getRentalId(),
                book.getBookId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                progress.getCurrentPage(),
                book.getTotalPages(),
                progressRate(progress.getCurrentPage(), book.getTotalPages()),
                progress.getLastReadAt()
        );
    }

    private double progressRate(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return 0;
        }
        return Math.min(100.0, currentPage * 100.0 / totalPages);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
