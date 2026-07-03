package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.ReadingProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReadingProgressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    private Long memberId;
    private Long rentalOldId;
    private Long rentalRecentId;
    private Long rentalNotStartedId;

    @BeforeEach
    void setUp() {
        Long publisherId = insertPublisher("Test Publisher");
        Long bookAId = insertBook("Java Basics", "1111111111111", publisherId);
        Long bookBId = insertBook("Spring Guide", "2222222222222", publisherId);
        Long bookCId = insertBook("Hidden Book", "3333333333333", publisherId);

        Long gradeId = insertMemberGrade("Seed Grade");
        memberId = insertMember("reader1", gradeId);
        Long otherMemberId = insertMember("reader2", gradeId);

        rentalOldId = insertBookRental(memberId, bookAId);
        rentalRecentId = insertBookRental(memberId, bookBId);
        rentalNotStartedId = insertBookRental(memberId, bookCId);
        Long otherMemberRentalId = insertBookRental(otherMemberId, bookAId);

        insertReadingProgress(rentalOldId, 10, LocalDateTime.now().minusDays(2));
        insertReadingProgress(rentalRecentId, 20, LocalDateTime.now().minusHours(1));
        insertReadingProgress(rentalNotStartedId, 1, null);
        insertReadingProgress(otherMemberRentalId, 5, LocalDateTime.now());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByRentalIdReturnsProgress() {
        Optional<ReadingProgress> progress = readingProgressRepository.findByRental_RentalId(rentalOldId);

        assertThat(progress).isPresent();
        assertThat(progress.get().getCurrentPage()).isEqualTo(10);
    }

    @Test
    void findRecentByMemberIdOrdersByLastReadAtDescAndExcludesOtherMembers() {
        List<ReadingProgress> recent = readingProgressRepository.findRecentByMemberId(memberId, PageRequest.of(0, 10));

        assertThat(recent).extracting(rp -> rp.getRental().getRentalId())
                .containsExactly(rentalRecentId, rentalOldId);
    }

    @Test
    void findRecentByMemberIdExcludesNotStartedRentals() {
        List<ReadingProgress> recent = readingProgressRepository.findRecentByMemberId(memberId, PageRequest.of(0, 10));

        assertThat(recent).extracting(rp -> rp.getRental().getRentalId())
                .doesNotContain(rentalNotStartedId);
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

    private void insertReadingProgress(Long rentalId, int currentPage, LocalDateTime lastReadAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO reading_progress (rental_id, current_page, read_page_count, " +
                        "last_read_at, completed_yn) VALUES (?, ?, ?, ?, false)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, rentalId)
                .setParameter(2, currentPage)
                .setParameter(3, currentPage)
                .setParameter(4, lastReadAt)
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
