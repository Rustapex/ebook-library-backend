package com.acorn.abc.collection.domain;

import com.acorn.abc.book.domain.Book;
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
@Table(name = "collection_book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_book_id")
    private Long collectionBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // collection_book에는 updated_at 컬럼이 없다(schema.sql 기준) — D-15: CollectionBook은
    // BaseEntity로 전환하지 않고 createdAt만 수동 관리하는 현행 필드를 유지한다.
    public static CollectionBook create(Collection collection, Book book, Integer displayOrder) {
        CollectionBook collectionBook = new CollectionBook();
        collectionBook.collection = collection;
        collectionBook.book = book;
        collectionBook.displayOrder = displayOrder;
        collectionBook.createdAt = LocalDateTime.now();
        return collectionBook;
    }

    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
