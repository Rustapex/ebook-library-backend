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
import java.math.BigDecimal;

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

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "epub_version")
    private Integer epubVersion;

    @Column(name = "location_cfi", columnDefinition = "TEXT")
    private String locationCfi;

    @Column(name = "location_hash", columnDefinition = "BINARY(32)")
    private byte[] locationHash;

    @Column(name = "chapter_href", length = 1000)
    private String chapterHref;

    @Column(name = "spine_index")
    private Integer spineIndex;

    @Column(name = "progression", precision = 6, scale = 5)
    private BigDecimal progression;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Bookmark create(BookRental rental, int pageNo) {
        Bookmark bookmark = new Bookmark();
        bookmark.rental = rental;
        bookmark.pageNo = pageNo;
        bookmark.createdAt = LocalDateTime.now();
        return bookmark;
    }

    public static Bookmark create(BookRental rental, int epubVersion, String cfi, byte[] locationHash,
                                  String chapterHref, Integer spineIndex, BigDecimal progression) {
        Bookmark bookmark = new Bookmark();
        bookmark.rental = rental;
        bookmark.epubVersion = epubVersion;
        bookmark.locationCfi = cfi;
        bookmark.locationHash = locationHash;
        bookmark.chapterHref = chapterHref;
        bookmark.spineIndex = spineIndex;
        bookmark.progression = progression;
        bookmark.createdAt = LocalDateTime.now();
        return bookmark;
    }
}
