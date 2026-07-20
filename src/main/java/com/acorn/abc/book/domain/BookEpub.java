package com.acorn.abc.book.domain;

import com.acorn.abc.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_epub", uniqueConstraints =
        @UniqueConstraint(name = "uk_book_epub_book_version", columnNames = {"book_id", "epub_version"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookEpub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_epub_id")
    private Long bookEpubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "epub_version", nullable = false)
    private int epubVersion;

    // epub_data는 큰 LONGBLOB이므로 이 metadata 엔티티에 매핑하지 않는다.
    // 업로드와 다운로드는 BookEpubBinaryStore에서 JDBC binary stream으로 처리한다.

    public Long getBookId() {
        return book.getBookId();
    }
}
