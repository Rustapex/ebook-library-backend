package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Long categoryChildId;
    private Long categoryRootId;
    private Long bookHiddenId;
    private Long bookSpringId;
    private Long collectionId;

    @BeforeEach
    void setUp() {
        Long publisherId = insertPublisher("Test Publisher");
        Long authorAId = insertAuthor("Author A");
        Long authorBId = insertAuthor("Author B");
        categoryRootId = insertCategory(null, "Root Category", 1);
        categoryChildId = insertCategory(categoryRootId, "Child Category", 1);

        Long bookJavaId = insertBook("Java Basics", "1111111111111", "FREE", 0, publisherId, "AVAILABLE");
        bookSpringId = insertBook("Spring Guide", "2222222222222", "PAID", 3000, publisherId, "AVAILABLE");
        bookHiddenId = insertBook("Hidden Book", "3333333333333", "FREE", 0, publisherId, "HIDDEN");

        insertBookAuthor(bookJavaId, authorAId, 0);
        insertBookAuthor(bookSpringId, authorBId, 0);
        insertBookCategory(bookSpringId, categoryChildId);

        collectionId = insertCollection("Spring Series", "SERIES", "ACTIVE");
        insertCollectionBook(collectionId, bookSpringId, 1);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void availableSpecificationExcludesHiddenBooks() {
        Page<Book> page = bookRepository.findAll(BookSpecifications.available(), PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Book::getBookId)
                .doesNotContain(bookHiddenId);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void hasParentCategoryFindsBooksUnderChildCategory() {
        Page<Book> page = bookRepository.findAll(
                BookSpecifications.available().and(BookSpecifications.hasParentCategory(categoryRootId)),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).extracting(Book::getBookId).containsExactly(bookSpringId);
    }

    @Test
    void searchMatchesByTitleAndAuthorName() {
        Page<Book> byTitle = bookRepository.search("%Spring%", null, null, PageRequest.of(0, 10));
        assertThat(byTitle.getContent()).extracting(Book::getBookId).containsExactly(bookSpringId);

        Page<Book> byAuthor = bookRepository.search("%Author B%", null, null, PageRequest.of(0, 10));
        assertThat(byAuthor.getContent()).extracting(Book::getBookId).containsExactly(bookSpringId);

        Page<Book> hiddenNeverMatches = bookRepository.search("%Hidden%", null, null, PageRequest.of(0, 10));
        assertThat(hiddenNeverMatches.getContent()).isEmpty();
    }

    @Test
    void findByCollectionIdReturnsOnlyAvailableBooksInActiveCollection() {
        Pageable unpaged = Pageable.unpaged();
        List<Book> books = bookRepository.findByCollectionId(collectionId, unpaged);

        assertThat(books).extracting(Book::getBookId).containsExactly(bookSpringId);
    }

    private Long insertPublisher(String name) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO publisher (publisher_name, status, created_at) VALUES (?, 'ACTIVE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("publisher").longValue();
    }

    private Long insertAuthor(String name) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO author (author_name, status, created_at) VALUES (?, 'ACTIVE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("author").longValue();
    }

    private Long insertCategory(Long parentId, String name, int displayOrder) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO category (parent_category_id, category_name, display_order, status) VALUES (?, ?, ?, 'ACTIVE')")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, parentId)
                .setParameter(2, name)
                .setParameter(3, displayOrder)
                .executeUpdate();
        return lastInsertId("category").longValue();
    }

    private Long insertBook(String title, String isbn, String rentalType, int rentalPrice, Long publisherId, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, " +
                        "default_rental_days, total_pages, rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, 7, 100, 0, 0.00, 0, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, publisherId)
                .setParameter(2, title)
                .setParameter(3, isbn)
                .setParameter(4, rentalType)
                .setParameter(5, rentalPrice)
                .setParameter(6, status)
                .setParameter(7, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book").longValue();
    }

    private void insertBookAuthor(Long bookId, Long authorId, int order) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_author (book_id, author_id, author_order) VALUES (?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, bookId)
                .setParameter(2, authorId)
                .setParameter(3, order)
                .executeUpdate();
    }

    private void insertBookCategory(Long bookId, Long categoryId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_category (book_id, category_id) VALUES (?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, bookId)
                .setParameter(2, categoryId)
                .executeUpdate();
    }

    private Long insertCollection(String name, String collectionType, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, collectionType)
                .setParameter(3, status)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("collection").longValue();
    }

    private void insertCollectionBook(Long collectionId, Long bookId, Integer displayOrder) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, collectionId)
                .setParameter(2, bookId)
                .setParameter(3, displayOrder)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
