package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.collection.domain.CollectionBook;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

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

    public static Specification<Book> hasParentCategory(Long parentCategoryId) {
        return (root, query, cb) -> {
            if (parentCategoryId == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BookCategory> bc = subquery.from(BookCategory.class);
            Join<Object, Object> category = bc.join("category");
            subquery.select(bc.get("book").get("bookId"))
                    .where(cb.and(
                            cb.equal(category.get("status"), ACTIVE),
                            cb.or(
                                    cb.equal(category.get("parent").get("categoryId"), parentCategoryId),
                                    cb.equal(category.get("categoryId"), parentCategoryId)
                            )
                    ));
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
