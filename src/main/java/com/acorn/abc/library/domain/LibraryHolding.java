package com.acorn.abc.library.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// row 존재 자체가 HELD를 의미한다. holding_status 컬럼은 두지 않는다(계획 3.3).
// 소장 관계도 Book.isbn과 같은 ISBN 값을 사용한다.
// createdAt/updatedAt은 BaseEntity(JPA Auditing)가 관리한다.
@Entity
@Table(name = "library_holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LibraryHolding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_holding_id")
    private Long libraryHoldingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Column(name = "isbn", nullable = false, length = 13)
    private String isbn;

    public static LibraryHolding create(Library library, String isbn) {
        LibraryHolding holding = new LibraryHolding();
        holding.library = library;
        holding.isbn = isbn;
        return holding;
    }
}
