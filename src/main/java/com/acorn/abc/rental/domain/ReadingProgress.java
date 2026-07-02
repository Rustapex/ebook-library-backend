package com.acorn.abc.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private BookRental rental;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "read_page_count", nullable = false)
    private int readPageCount;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "completed_yn", nullable = false)
    private Boolean completedYn;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static ReadingProgress start(BookRental rental) {
        ReadingProgress progress = new ReadingProgress();
        progress.rental = rental;
        progress.currentPage = 1;
        progress.readPageCount = 0;
        progress.completedYn = false;
        return progress;
    }

    public void moveToPage(int pageNo, LocalDateTime viewedAt) {
        this.currentPage = pageNo;
        this.readPageCount = Math.max(this.readPageCount, pageNo);
        this.lastReadAt = viewedAt;
    }

    // 마지막 페이지 도달 시 최초 1회만 완독 처리(api-spec 4.3절)
    public boolean completeIfLastPage(int totalPages, LocalDateTime completedAt) {
        if (Boolean.TRUE.equals(this.completedYn) || this.currentPage < totalPages) {
            return false;
        }
        this.completedYn = true;
        this.completedAt = completedAt;
        return true;
    }
}
