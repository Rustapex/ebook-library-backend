package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.collection.domain.CollectionBook;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class BookSpecifications {

    private static final String AVAILABLE = "AVAILABLE";
    private static final String ACTIVE = "ACTIVE";
    private static final String EVENT = "EVENT";

    private BookSpecifications() {
    }

    public static Specification<Book> available() {
        return (root, query, cb) -> cb.equal(root.get("status"), AVAILABLE);
    }

    public static Specification<Book> hasRentalType(String rentalType) {
        return (root, query, cb) -> rentalType == null
                ? cb.conjunction()
                : cb.equal(root.get("rentalType"), rentalType);
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

    // categoryIds는 CategoryService.findDescendantIds()로 미리 계산한 (자기 자신 포함) 하위 카테고리 id 전체다.
    // null이면 필터링하지 않고(요청 자체에 parentCategoryId 없음), 빈 리스트면 아무것도 매칭하지 않는다
    // (parentCategoryId가 존재하지 않거나 비활성 카테고리라 하위가 없는 경우).
    public static Specification<Book> hasAnyCategory(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null) {
                return cb.conjunction();
            }
            if (categoryIds.isEmpty()) {
                return cb.disjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BookCategory> bc = subquery.from(BookCategory.class);
            subquery.select(bc.get("book").get("bookId"))
                    .where(bc.get("category").get("categoryId").in(categoryIds));
            return root.get("bookId").in(subquery);
        };
    }

    public static Specification<Book> inCollection(Long collectionId) {
        return (root, query, cb) -> {
            if (collectionId == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CollectionBook> cbk = subquery.from(CollectionBook.class);
            Join<Object, Object> collection = cbk.join("collection");
            subquery.select(cbk.get("book").get("bookId"))
                    .where(cb.and(
                            cb.equal(collection.get("status"), ACTIVE),
                            cb.equal(collection.get("collectionId"), collectionId)
                    ));
            return root.get("bookId").in(subquery);
        };
    }

    public static Specification<Book> inEventSection(boolean eventOnly) {
        return (root, query, cb) -> {
            if (!eventOnly) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CollectionBook> cbk = subquery.from(CollectionBook.class);
            Join<Object, Object> collection = cbk.join("collection");
            subquery.select(cbk.get("book").get("bookId"))
                    .where(cb.and(
                            cb.equal(collection.get("status"), ACTIVE),
                            cb.equal(collection.get("collectionType"), EVENT)
                    ));
            return root.get("bookId").in(subquery);
        };
    }
}
