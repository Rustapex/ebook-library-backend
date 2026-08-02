package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookAuthor;
import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.book.domain.Publisher;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 도서 목록(API-ADMIN-BOOK-001) 전용 검색 조건.
 * 전부 EXISTS/IN 서브쿼리 또는 단순 컬럼 비교로만 구성해 book 행이 늘어나지 않게 한다
 * (관리자 도서 API 설계문서 §5 — collection fetch join으로 페이지를 만들지 않음).
 * 공개 카탈로그의 {@link BookSpecifications}는 수정하지 않고 이 클래스만 새로 둔다.
 */
public final class AdminBookSpecifications {

    private AdminBookSpecifications() {
    }

    public static Specification<Book> hasStatus(String status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Book> hasRentalType(String rentalType) {
        return (root, query, cb) -> rentalType == null ? cb.conjunction() : cb.equal(root.get("rentalType"), rentalType);
    }

    public static Specification<Book> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BookCategory> bc = subquery.from(BookCategory.class);
            subquery.select(bc.get("book").get("bookId"))
                    .where(cb.equal(bc.get("category").get("categoryId"), categoryId));
            return root.get("bookId").in(subquery);
        };
    }

    // likeLower는 호출부(AdminBookService)에서 소문자 + '%' 래핑까지 끝낸 값을 넘긴다
    // (MemberRepository.searchForAdmin과 동일 관례). 제목/ISBN/출판사명/저자명을 OR로 검색한다.
    public static Specification<Book> matchesKeyword(String likeLower) {
        return (root, query, cb) -> {
            if (likeLower == null) {
                return cb.conjunction();
            }
            Join<Book, Publisher> publisher = root.join("publisher", JoinType.LEFT);

            Subquery<Long> authorSubquery = query.subquery(Long.class);
            Root<BookAuthor> ba = authorSubquery.from(BookAuthor.class);
            authorSubquery.select(ba.get("book").get("bookId"))
                    .where(cb.like(cb.lower(ba.get("author").get("authorName")), likeLower));

            Predicate titleMatch = cb.like(cb.lower(root.get("title")), likeLower);
            Predicate isbnMatch = cb.like(cb.lower(root.get("isbn")), likeLower);
            Predicate publisherMatch = cb.like(cb.lower(publisher.get("publisherName")), likeLower);
            Predicate authorMatch = root.get("bookId").in(authorSubquery);

            return cb.or(titleMatch, isbnMatch, publisherMatch, authorMatch);
        };
    }
}
