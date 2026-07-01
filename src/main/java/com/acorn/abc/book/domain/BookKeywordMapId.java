package com.acorn.abc.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class BookKeywordMapId implements Serializable {

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "keyword_id")
    private Long keywordId;

    public BookKeywordMapId(Long bookId, Long keywordId) {
        this.bookId = bookId;
        this.keywordId = keywordId;
    }
}
