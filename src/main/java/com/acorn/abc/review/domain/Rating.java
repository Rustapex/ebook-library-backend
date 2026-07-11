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
@Table(name = "rating")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "rating_score", nullable = false)
    private byte ratingScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 생성 후 수정/삭제 불가 - 변경 메서드를 두지 않는다
    public static Rating create(Member member, Book book, int ratingScore) {
        Rating rating = new Rating();
        rating.member = member;
        rating.book = book;
        rating.ratingScore = (byte) ratingScore;
        rating.createdAt = LocalDateTime.now();
        return rating;
    }
}
