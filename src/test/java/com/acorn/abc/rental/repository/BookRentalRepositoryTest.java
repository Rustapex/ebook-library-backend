package com.acorn.abc.rental.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 통계 확장(PR #42 리뷰 Watchlist W2) — countByStatisticConditions/그룹화 쿼리 검증.
 * 실제 공유 MySQL DB(다른 테스트/시드 데이터 포함)를 사용하므로, 절대 건수 대신
 * "테스트 데이터 삽입 전/후 차이"로 검증한다(CLAUDE.md 검증 규칙 — 실 DB 테스트 격리 이슈 반영).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRentalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRentalRepository bookRentalRepository;

    private Long memberId;
    private Long freeBookId;
    private Long paidBookId;

    private long baselineTotalCount;
    private long baselineFreeCount;
    private long baselinePaidCount;
    private long baselinePeriodCount;

    @BeforeEach
    void setUp() {
        baselineTotalCount = bookRentalRepository.countByStatisticConditions(null, null, null, null, null);
        baselineFreeCount = bookRentalRepository.countByStatisticConditions(null, null, "FREE", null, null);
        baselinePaidCount = bookRentalRepository.countByStatisticConditions(null, null, "PAID", null, null);
        baselinePeriodCount = bookRentalRepository.countByStatisticConditions(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0), null, null, null);

        memberId = insertMember("rental_stat_user", LocalDate.of(2000, 1, 1));
        freeBookId = insertBook("무료 도서", "FREE");
        paidBookId = insertBook("유료 도서", "PAID");

        // 2026-01-15: 기간 안(경계 시작 포함 검증용)
        insertRental(memberId, freeBookId, LocalDateTime.of(2026, 1, 15, 10, 0));
        // 2026-01-31 23:59:59: 기간 끝 직전(포함)
        insertRental(memberId, paidBookId, LocalDateTime.of(2026, 1, 31, 23, 59, 59));
        // 2026-02-01 00:00:00: 다음 기간 시작(제외 — endExclusive 경계 검증용)
        insertRental(memberId, freeBookId, LocalDateTime.of(2026, 2, 1, 0, 0));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void countByStatisticConditions_null_필터는_전체_건수를_반환한다() {
        long count = bookRentalRepository.countByStatisticConditions(null, null, null, null, null);

        assertThat(count).isEqualTo(baselineTotalCount + 3);
    }

    @Test
    void countByStatisticConditions_기간은_시작포함_끝미포함이다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);

        long count = bookRentalRepository.countByStatisticConditions(from, to, null, null, null);

        // 1/15, 1/31 23:59:59 포함, 2/1 00:00:00은 endExclusive라 제외 -> 기준선 + 2건
        assertThat(count).isEqualTo(baselinePeriodCount + 2);
    }

    @Test
    void countByStatisticConditions_대여유형으로_FREE_PAID를_분리_집계한다() {
        long freeCount = bookRentalRepository.countByStatisticConditions(null, null, "FREE", null, null);
        long paidCount = bookRentalRepository.countByStatisticConditions(null, null, "PAID", null, null);

        assertThat(freeCount).isEqualTo(baselineFreeCount + 2);
        assertThat(paidCount).isEqualTo(baselinePaidCount + 1);
    }

    @Test
    void countByStatisticConditions_나이대_경계는_생일_당일을_포함한다() {
        // 20S: birthDate <= baseDate-20y(2006-07-12) AND birthDate > baseDate-30y(1996-07-12)
        LocalDate birthDateFrom = LocalDate.of(1996, 7, 12);
        LocalDate birthDateTo = LocalDate.of(2006, 7, 12);
        long baseline = bookRentalRepository.countByStatisticConditions(null, null, null, birthDateFrom, birthDateTo);

        Long boundaryMemberId = insertMember("rental_stat_boundary", birthDateTo);
        insertRental(boundaryMemberId, freeBookId, LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.flush();
        entityManager.clear();

        long count = bookRentalRepository.countByStatisticConditions(null, null, null, birthDateFrom, birthDateTo);

        assertThat(count).isEqualTo(baseline + 1);
    }

    @Test
    void groupByDayStatisticConditions_해당_날짜_bucket에_정확한_건수가_들어간다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 2, 0, 0);

        var buckets = bookRentalRepository.groupByDayStatisticConditions(from, to, null, null, null);

        var jan15 = buckets.stream().filter(b -> b.getBucketDate().equals(LocalDate.of(2026, 1, 15))).findFirst().orElseThrow();
        assertThat(jan15.getBucketCount()).isEqualTo(1L);
        var jan31 = buckets.stream().filter(b -> b.getBucketDate().equals(LocalDate.of(2026, 1, 31))).findFirst().orElseThrow();
        assertThat(jan31.getBucketCount()).isEqualTo(1L);
    }

    @Test
    void groupByMonthStatisticConditions_해당_월_bucket에_정확한_건수가_들어간다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        long baselineJanuary = bookRentalRepository.groupByMonthStatisticConditions(from, to, null, null, null)
                .stream().filter(b -> "2026-01".equals(b.getBucketMonth()))
                .mapToLong(BookRentalRepository.MonthlyCountBucket::getBucketCount).sum();

        var buckets = bookRentalRepository.groupByMonthStatisticConditions(from, to, null, null, null);
        var january = buckets.stream().filter(b -> "2026-01".equals(b.getBucketMonth())).findFirst().orElseThrow();

        assertThat(january.getBucketCount()).isEqualTo(baselineJanuary);
        assertThat(january.getBucketCount()).isGreaterThanOrEqualTo(2L);
    }

    // 회원용독서통계 구현계획 §5.2 — 회원 단위 대여 수 집계. setUp()에서 만든 memberId(이 테스트 전용)만
    // 사용하므로 공유 DB라도 절대값 assertion이 안전하다.
    @Test
    void countByMemberAndPeriod_null_필터는_해당_회원_전체_건수를_반환한다() {
        long count = bookRentalRepository.countByMemberAndPeriod(memberId, null, null);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countByMemberAndPeriod_기간은_시작포함_끝미포함이다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);

        long count = bookRentalRepository.countByMemberAndPeriod(memberId, from, to);

        // 1/15, 1/31 23:59:59 포함, 2/1 00:00:00은 endExclusive라 제외
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countByMemberAndPeriod_다른_회원의_대여는_포함하지_않는다() {
        Long otherMemberId = insertMember("rental_stat_other", LocalDate.of(2000, 1, 1));
        insertRental(otherMemberId, freeBookId, LocalDateTime.of(2026, 1, 20, 0, 0));
        entityManager.flush();
        entityManager.clear();

        long count = bookRentalRepository.countByMemberAndPeriod(memberId, null, null);

        assertThat(count).isEqualTo(3L);
    }

    private Long insertMember(String loginId, LocalDate birthDate) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', 'Tester', ?, ?, 'USER', 1, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId + "@test.com")
                .setParameter(3, birthDate)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member");
    }

    private Long insertBook(String title, String rentalType) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (title, rental_type, rental_price, default_rental_days, total_pages, " +
                        "rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, ?, 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, title)
                .setParameter(2, rentalType)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book");
    }

    private void insertRental(Long memberId, Long bookId, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_rental (member_id, book_id, rental_end_at, rental_status, created_at) " +
                        "VALUES (?, ?, ?, 'READING', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, createdAt.plusDays(7))
                .setParameter(4, createdAt)
                .executeUpdate();
    }

    private Long lastInsertId(String table) {
        String idColumn = table + "_id";
        Number number = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
        return number.longValue();
    }
}
