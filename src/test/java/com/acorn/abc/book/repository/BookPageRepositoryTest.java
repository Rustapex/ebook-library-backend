package com.acorn.abc.book.repository;

import com.acorn.abc.book.domain.BookPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookPageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookPageRepository bookPageRepository;

    private Long bookId;

    @BeforeEach
    void setUp() {
        Long publisherId = insertPublisher("Test Publisher");
        bookId = insertBook("Java Basics", "1111111111111", publisherId);
        insertBookPage(bookId, 1, "Page one content");
        insertBookPage(bookId, 2, "Page two content");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByBookIdAndPageNoReturnsMatchingPage() {
        Optional<BookPage> page = bookPageRepository.findByBook_BookIdAndPageNo(bookId, 2);

        assertThat(page).isPresent();
        assertThat(page.get().getPageContent()).isEqualTo("Page two content");
    }

    @Test
    void findByBookIdAndPageNoReturnsEmptyForMissingPage() {
        Optional<BookPage> page = bookPageRepository.findByBook_BookIdAndPageNo(bookId, 99);

        assertThat(page).isEmpty();
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

    private Long insertBook(String title, String isbn, Long publisherId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, " +
                        "default_rental_days, total_pages, rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, ?, ?, 'FREE', 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, publisherId)
                .setParameter(2, title)
                .setParameter(3, isbn)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book").longValue();
    }

    private void insertBookPage(Long bookId, int pageNo, String content) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_page (book_id, page_no, page_content, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, bookId)
                .setParameter(2, pageNo)
                .setParameter(3, content)
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
