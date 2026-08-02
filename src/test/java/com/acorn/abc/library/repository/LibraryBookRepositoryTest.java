package com.acorn.abc.library.repository;

import com.acorn.abc.library.domain.LibraryBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LibraryBookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    private Long bookId;
    private Long activeLibraryAId;
    private Long inactiveLibraryId;

    @BeforeEach
    void setUp() {
        Long publisherId = insertPublisher("Test Publisher");
        bookId = insertBook("Java Basics", "1111111111111", publisherId);
        Long otherBookId = insertBook("Spring Guide", "2222222222222", publisherId);

        activeLibraryAId = insertLibrary("B Library", "ACTIVE");
        Long activeLibraryBId = insertLibrary("A Library", "ACTIVE");
        inactiveLibraryId = insertLibrary("Closed Library", "INACTIVE");

        insertLibraryBook(activeLibraryAId, bookId, "AVAILABLE");
        insertLibraryBook(activeLibraryBId, bookId, "UNAVAILABLE");
        insertLibraryBook(inactiveLibraryId, bookId, "AVAILABLE");
        insertLibraryBook(activeLibraryAId, otherBookId, "AVAILABLE");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findActiveByBookIdExcludesInactiveLibrariesAndOtherBooks() {
        List<LibraryBook> result = libraryBookRepository.findActiveByBookId(bookId);

        assertThat(result)
                .extracting(lb -> lb.getLibrary().getLibraryId())
                .doesNotContain(inactiveLibraryId);
        assertThat(result).extracting(lb -> lb.getBook().getBookId()).containsOnly(bookId);
    }

    @Test
    void findActiveByBookIdOrdersByLibraryNameAsc() {
        List<LibraryBook> result = libraryBookRepository.findActiveByBookId(bookId);

        assertThat(result).extracting(lb -> lb.getLibrary().getLibraryName())
                .containsExactly("A Library", "B Library");
    }

    @Test
    void findActiveByBookIdKeepsHoldingStatusAsIs() {
        List<LibraryBook> result = libraryBookRepository.findActiveByBookId(bookId);

        assertThat(result)
                .extracting(LibraryBook::getHoldingStatus)
                .containsExactlyInAnyOrder("AVAILABLE", "UNAVAILABLE");
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

    private Long insertLibrary(String name, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO library (library_name, address, latitude, longitude, status) " +
                        "VALUES (?, 'Seoul', 37.5, 127.0, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, status)
                .executeUpdate();
        return lastInsertId("library").longValue();
    }

    private void insertLibraryBook(Long libraryId, Long bookId, String holdingStatus) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO library_book (library_id, book_id, holding_status, updated_at) " +
                        "VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, libraryId)
                .setParameter(2, bookId)
                .setParameter(3, holdingStatus)
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
