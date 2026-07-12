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

    // 관리자 통계 확장(PR #42 리뷰 Watchlist W2) — 완독 기간/나이대 조건부 집계 검증.
    // 실제 공유 MySQL DB를 사용하므로 절대 건수 대신 삽입 전/후 차이로 검증한다.
    @Test
    void countCompletedByStatisticConditions_완독여부와_기간_나이대_조건을_함께_적용한다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        java.time.LocalDate birthDateFrom = java.time.LocalDate.of(1996, 7, 12);
        java.time.LocalDate birthDateTo = java.time.LocalDate.of(2006, 7, 12);
        long baseline = readingProgressRepository.countCompletedByStatisticConditions(from, to, birthDateFrom, birthDateTo);

        Long publisherId = insertPublisher("Stat Publisher");
        Long bookId = insertBook("Stat Book", "9999999999999", publisherId);
        // grade_id=1은 다른 repository 테스트에서도 공통으로 쓰는 시드 기본 등급이다.
        Long boundaryMemberId = insertMemberWithBirthDate("stat_reader_boundary", 1L, "2006-07-12");

        Long inRangeRentalId = insertBookRental(boundaryMemberId, bookId);
        insertCompletedReadingProgress(inRangeRentalId, LocalDateTime.of(2026, 1, 15, 10, 0));
        Long outOfRangeRentalId = insertBookRental(boundaryMemberId, insertBook("Stat Book 2", "8888888888888", publisherId));
        insertCompletedReadingProgress(outOfRangeRentalId, LocalDateTime.of(2026, 2, 1, 0, 0));

        entityManager.flush();
        entityManager.clear();

        // 20S 나이대: birthDate <= 2006-07-12, birthDate > 1996-07-12
        long count = readingProgressRepository.countCompletedByStatisticConditions(from, to, birthDateFrom, birthDateTo);

        // inRangeRentalId(1/15, 기간+나이대 조건 부합)만 포함, outOfRangeRentalId(2/1)는 endExclusive라 제외
        assertThat(count).isEqualTo(baseline + 1);
    }

    private Long insertMemberWithBirthDate(String loginId, Long gradeId, String birthDate) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, " +
                        "grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', ?, ?, ?, 'USER', ?, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId)
                .setParameter(3, loginId + "@test.com")
                .setParameter(4, java.time.LocalDate.parse(birthDate))
                .setParameter(5, gradeId)
                .setParameter(6, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member").longValue();
    }

    private void insertCompletedReadingProgress(Long rentalId, LocalDateTime completedAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO reading_progress (rental_id, current_page, read_page_count, " +
                        "completed_yn, completed_at) VALUES (?, 100, 100, true, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, rentalId)
                .setParameter(2, completedAt)
                .executeUpdate();
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
