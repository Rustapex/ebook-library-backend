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
}
