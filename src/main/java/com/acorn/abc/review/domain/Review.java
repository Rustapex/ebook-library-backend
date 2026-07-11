package com.acorn.abc.review.domain;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.member.domain.Member;
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
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_DELETED = "DELETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Review create(Member member, Book book, String content) {
        Review review = new Review();
        review.member = member;
        review.book = book;
        review.content = content;
        review.status = STATUS_ACTIVE;
        review.createdAt = LocalDateTime.now();
        return review;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(this.status);
    }

    public boolean isHidden() {
        return STATUS_HIDDEN.equals(this.status);
    }

    public boolean isDeleted() {
        return STATUS_DELETED.equals(this.status);
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    // DELETED -> ACTIVE 복구 전용. 같은 row를 재사용하고 내용을 새 값으로 갱신한다
    public void restore(String content) {
        this.status = STATUS_ACTIVE;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.status = STATUS_DELETED;
        this.updatedAt = LocalDateTime.now();
    }
}
