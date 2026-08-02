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

    // 관리자API후속작업 구현계획 §6 — completedReadTrend 집계 쿼리(PR #42 리뷰 항목 2) 직접 검증.
    // 실제 공유 MySQL DB이므로 절대 건수 대신 삽입 전/후 차이로 검증한다(countCompletedByStatisticConditions와 동일 패턴).
    @Test
    void groupByDayCompletedStatisticConditions_완독_수를_일별로_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);
        java.time.LocalDate targetDate = java.time.LocalDate.of(2026, 3, 10);

        long baseline = bucketCountForDate(
                readingProgressRepository.groupByDayCompletedStatisticConditions(from, to, null, null), targetDate);

        Long publisherId = insertPublisher("Trend Publisher Daily");
        Long bookId = insertBook("Trend Book Daily", "6666666666666", publisherId);
        // grade_id=1L: grade_level에 UNIQUE 제약이 있어 insertMemberGrade()를 또 호출하면 setUp()의 "Seed Grade"(level 99)와 충돌한다.
        Long trendMemberId = insertMember("trend_reader_daily", 1L);
        Long rentalId = insertBookRental(trendMemberId, bookId);
        insertCompletedReadingProgress(rentalId, LocalDateTime.of(2026, 3, 10, 15, 0));

        entityManager.flush();
        entityManager.clear();

        long afterCount = bucketCountForDate(
                readingProgressRepository.groupByDayCompletedStatisticConditions(from, to, null, null), targetDate);

        assertThat(afterCount).isEqualTo(baseline + 1);
    }

    @Test
    void groupByMonthCompletedStatisticConditions_완독_수를_월별로_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);
        String targetMonth = "2026-03";

        long baseline = bucketCountForMonth(
                readingProgressRepository.groupByMonthCompletedStatisticConditions(from, to, null, null), targetMonth);

        Long publisherId = insertPublisher("Trend Publisher Monthly");
        Long bookId = insertBook("Trend Book Monthly", "5555555555555", publisherId);
        Long trendMemberId = insertMember("trend_reader_monthly", 1L);
        Long rentalId = insertBookRental(trendMemberId, bookId);
        insertCompletedReadingProgress(rentalId, LocalDateTime.of(2026, 3, 20, 9, 0));

        entityManager.flush();
        entityManager.clear();

        long afterCount = bucketCountForMonth(
                readingProgressRepository.groupByMonthCompletedStatisticConditions(from, to, null, null), targetMonth);

        assertThat(afterCount).isEqualTo(baseline + 1);
    }

    // 회원용독서통계 구현계획 §5.2 — 회원 단위(memberId) + 기간 조건부 완독 수 집계.
    // 이 테스트에서 새로 만든 회원(statMemberId)만 사용하므로, 공유 DB라도 절대값 assertion이 안전하다.
    @Test
    void countCompletedByMemberAndPeriod_회원과_기간으로_완독_수를_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 0, 0);

        Long publisherId = insertPublisher("Member Stat Publisher");
        Long bookId = insertBook("Member Stat Book", "4000000000001", publisherId);
        Long statMemberId = insertMember("member_stat_reader", 1L);

        Long inRangeRentalId = insertBookRental(statMemberId, bookId);
        insertCompletedReadingProgress(inRangeRentalId, LocalDateTime.of(2026, 5, 15, 10, 0));
        Long outOfRangeRentalId = insertBookRental(statMemberId, insertBook("Member Stat Book 2", "4000000000002", publisherId));
        insertCompletedReadingProgress(outOfRangeRentalId, LocalDateTime.of(2026, 6, 1, 0, 0));

        entityManager.flush();
        entityManager.clear();

        long count = readingProgressRepository.countCompletedByMemberAndPeriod(statMemberId, from, to);

        // 5/15(기간 안)만 포함, 6/1 00:00:00(endExclusive)은 제외
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void countCompletedByMemberAndPeriod_다른_회원의_완독은_포함하지_않는다() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 0, 0);

        Long publisherId = insertPublisher("Isolation Publisher");
        Long targetMemberId = insertMember("isolation_target", 1L);
        Long otherMemberId2 = insertMember("isolation_other", 1L);

        Long targetRentalId = insertBookRental(targetMemberId, insertBook("Isolation Book 1", "4000000000003", publisherId));
        insertCompletedReadingProgress(targetRentalId, LocalDateTime.of(2026, 5, 10, 10, 0));
        Long otherRentalId = insertBookRental(otherMemberId2, insertBook("Isolation Book 2", "4000000000004", publisherId));
        insertCompletedReadingProgress(otherRentalId, LocalDateTime.of(2026, 5, 10, 10, 0));

        entityManager.flush();
        entityManager.clear();

        long count = readingProgressRepository.countCompletedByMemberAndPeriod(targetMemberId, from, to);

        assertThat(count).isEqualTo(1L);
    }

    // 회원용독서통계 구현계획 §4.2 B1 확정 — last_read_at이 기간 안에 있는 행의 read_page_count만 합산되는지 검증.
    @Test
    void sumReadPageCountByMemberAndPeriod_lastReadAt이_기간_안에_있는_행만_합산한다() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 0, 0);

        Long publisherId = insertPublisher("Page Sum Publisher");
        Long statMemberId = insertMember("page_sum_reader", 1L);

        Long inRangeRentalId = insertBookRental(statMemberId, insertBook("Page Sum Book 1", "4000000000005", publisherId));
        insertReadingProgress(inRangeRentalId, 120, LocalDateTime.of(2026, 5, 15, 9, 0));
        Long outOfRangeRentalId = insertBookRental(statMemberId, insertBook("Page Sum Book 2", "4000000000006", publisherId));
        insertReadingProgress(outOfRangeRentalId, 80, LocalDateTime.of(2026, 6, 1, 0, 0));

        entityManager.flush();
        entityManager.clear();

        long sum = readingProgressRepository.sumReadPageCountByMemberAndPeriod(statMemberId, from, to);

        // 5/15(기간 안) 120만 합산, 6/1 00:00:00(endExclusive)의 80은 제외
        assertThat(sum).isEqualTo(120L);
    }

    @Test
    void groupByDayCompletedByMember_회원_기준으로_일별_완독_수를_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 0, 0);

        Long publisherId = insertPublisher("Member Trend Publisher Daily");
        Long statMemberId = insertMember("member_trend_daily", 1L);
        Long rentalId = insertBookRental(statMemberId, insertBook("Member Trend Book Daily", "4000000000007", publisherId));
        insertCompletedReadingProgress(rentalId, LocalDateTime.of(2026, 5, 10, 15, 0));

        entityManager.flush();
        entityManager.clear();

        var buckets = readingProgressRepository.groupByDayCompletedByMember(statMemberId, from, to);

        long may10Count = bucketCountForDate(buckets, java.time.LocalDate.of(2026, 5, 10));
        assertThat(may10Count).isEqualTo(1L);
    }

    @Test
    void groupByMonthCompletedByMember_회원_기준으로_월별_완독_수를_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 0, 0);

        Long publisherId = insertPublisher("Member Trend Publisher Monthly");
        Long statMemberId = insertMember("member_trend_monthly", 1L);
        Long rentalId = insertBookRental(statMemberId, insertBook("Member Trend Book Monthly", "4000000000008", publisherId));
        insertCompletedReadingProgress(rentalId, LocalDateTime.of(2026, 5, 20, 9, 0));

        entityManager.flush();
        entityManager.clear();

        var buckets = readingProgressRepository.groupByMonthCompletedByMember(statMemberId, from, to);

        long mayCount = bucketCountForMonth(buckets, "2026-05");
        assertThat(mayCount).isEqualTo(1L);
    }

    private long bucketCountForDate(List<ReadingProgressRepository.DailyCountBucket> buckets, java.time.LocalDate date) {
        return buckets.stream()
                .filter(b -> date.equals(b.getBucketDate()))
                .mapToLong(ReadingProgressRepository.DailyCountBucket::getBucketCount)
                .findFirst()
                .orElse(0L);
    }

    private long bucketCountForMonth(List<ReadingProgressRepository.MonthlyCountBucket> buckets, String month) {
        return buckets.stream()
                .filter(b -> month.equals(b.getBucketMonth()))
                .mapToLong(ReadingProgressRepository.MonthlyCountBucket::getBucketCount)
                .findFirst()
                .orElse(0L);
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
