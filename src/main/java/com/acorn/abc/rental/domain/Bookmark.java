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
@Table(name = "bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private BookRental rental;

    @Column(name = "page_no", nullable = false)
    private int pageNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Bookmark create(BookRental rental, int pageNo) {
        Bookmark bookmark = new Bookmark();
        bookmark.rental = rental;
        bookmark.pageNo = pageNo;
        bookmark.createdAt = LocalDateTime.now();
        return bookmark;
    }
}
