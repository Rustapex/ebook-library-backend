package com.acorn.abc.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_isbn_temp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookIsbnTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "isbn13", nullable = false, length = 13)
    private String isbn13;

    @Column(name = "book_title", length = 255)
    private String bookTitle;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "pub_date", length = 20)
    private String pubDate;

    @Column(name = "aladin_category_id", length = 20)
    private String aladinCategoryId;

    @Column(name = "naver_res_status", nullable = false)
    private byte naverResStatus;

    @Column(name = "aladin_res_status", nullable = false)
    private byte aladinResStatus;

    @Column(name = "status_cd", nullable = false)
    private byte statusCd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
