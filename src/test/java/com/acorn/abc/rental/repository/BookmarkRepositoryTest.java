package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookmarkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    private Long rentalId;
    private Long otherRentalId;
    private Long bookmarkPage5Id;

    @BeforeEach
    void setUp() {
        Long publisherId = insertPublisher("Test Publisher");
        Long bookId = insertBook("Java Basics", "1111111111111", publisherId);

        Long gradeId = insertMemberGrade("Seed Grade");
        Long memberId = insertMember("reader1", gradeId);

        rentalId = insertBookRental(memberId, bookId);
        otherRentalId = insertBookRental(memberId, bookId);

        insertBookmark(rentalId, 10);
        bookmarkPage5Id = insertBookmark(rentalId, 5);
        insertBookmark(otherRentalId, 1);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByRentalIdAndPageNoDetectsExistingBookmark() {
        assertThat(bookmarkRepository.existsByRental_RentalIdAndPageNo(rentalId, 10)).isTrue();
        assertThat(bookmarkRepository.existsByRental_RentalIdAndPageNo(rentalId, 99)).isFalse();
    }

    @Test
    void findByBookmarkIdAndRentalIdRejectsOtherRental() {
        Optional<Bookmark> found = bookmarkRepository.findByBookmarkIdAndRental_RentalId(bookmarkPage5Id, otherRentalId);

        assertThat(found).isEmpty();
    }

    @Test
    void findByRentalIdOrderByPageNoAscReturnsAscendingOrder() {
        List<Bookmark> bookmarks = bookmarkRepository.findByRental_RentalIdOrderByPageNoAsc(rentalId);

        assertThat(bookmarks).extracting(Bookmark::getPageNo).containsExactly(5, 10);
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

    private Long insertMemberGrade(String name) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member_grade (grade_name, grade_level, min_payment_amount, " +
                        "grade_period_months, maintain_payment_rate) VALUES (?, 99, 0, 0, 10.00)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .executeUpdate();
        Number gradeId = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(grade_id) FROM member_grade")
                .getSingleResult();
        return gradeId.longValue();
    }

    private Long insertMember(String loginId, Long gradeId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, " +
                        "grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', ?, ?, '2000-01-01', 'USER', ?, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId)
                .setParameter(3, loginId + "@test.com")
                .setParameter(4, gradeId)
                .setParameter(5, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member").longValue();
    }

    private Long insertBookRental(Long memberId, Long bookId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_rental (member_id, book_id, rental_end_at, rental_status, created_at) " +
                        "VALUES (?, ?, ?, 'READING', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, LocalDateTime.now().plusDays(7))
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        Number rentalId = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(rental_id) FROM book_rental")
                .getSingleResult();
        return rentalId.longValue();
    }

    private Long insertBookmark(Long rentalId, int pageNo) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO bookmark (rental_id, page_no, created_at) VALUES (?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, rentalId)
                .setParameter(2, pageNo)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("bookmark").longValue();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
