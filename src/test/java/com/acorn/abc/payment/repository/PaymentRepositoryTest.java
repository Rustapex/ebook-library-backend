package com.acorn.abc.payment.repository;

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
 * 관리자 통계 확장(PR #42 리뷰 Watchlist W2) — sumAmountByStatisticConditions/그룹화 쿼리 검증.
 * 실제 공유 MySQL DB를 사용하므로 절대 합계 대신 "삽입 전/후 차이"로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private Long memberId;
    private Long rentalId;

    private long baselineTotalSum;
    private long baselinePeriodSum;

    @BeforeEach
    void setUp() {
        baselineTotalSum = paymentRepository.sumAmountByStatisticConditions("PAID", null, null, null, null);
        baselinePeriodSum = paymentRepository.sumAmountByStatisticConditions(
                "PAID", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0), null, null);

        memberId = insertMember("payment_stat_user", LocalDate.of(2000, 1, 1));
        Long bookId = insertBook();
        rentalId = insertRental(memberId, bookId);

        insertPayment(memberId, rentalId, 1000, LocalDateTime.of(2026, 1, 15, 10, 0));
        insertPayment(memberId, rentalId, 2000, LocalDateTime.of(2026, 1, 31, 23, 59, 59));
        insertPayment(memberId, rentalId, 3000, LocalDateTime.of(2026, 2, 1, 0, 0));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void sumAmountByStatisticConditions_null_필터는_PAID_전체_합계를_반환한다() {
        long sum = paymentRepository.sumAmountByStatisticConditions("PAID", null, null, null, null);

        assertThat(sum).isEqualTo(baselineTotalSum + 6000L);
    }

    @Test
    void sumAmountByStatisticConditions_기간은_시작포함_끝미포함이다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);

        long sum = paymentRepository.sumAmountByStatisticConditions("PAID", from, to, null, null);

        // 1000 + 2000 포함, 2/1 00:00:00 결제 3000은 endExclusive라 제외
        assertThat(sum).isEqualTo(baselinePeriodSum + 3000L);
    }

    @Test
    void sumAmountByStatisticConditions_나이대_경계는_생일_당일을_포함한다() {
        LocalDate birthDateFrom = LocalDate.of(1996, 7, 12);
        LocalDate birthDateTo = LocalDate.of(2006, 7, 12);
        long baseline = paymentRepository.sumAmountByStatisticConditions("PAID", null, null, birthDateFrom, birthDateTo);

        Long boundaryMemberId = insertMember("payment_stat_boundary", birthDateTo);
        Long boundaryRentalId = insertRental(boundaryMemberId, insertBook());
        insertPayment(boundaryMemberId, boundaryRentalId, 500, LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.flush();
        entityManager.clear();

        long sum = paymentRepository.sumAmountByStatisticConditions("PAID", null, null, birthDateFrom, birthDateTo);

        assertThat(sum).isEqualTo(baseline + 500L);
    }

    @Test
    void groupByMonthStatisticConditions_해당_월_bucket에_정확한_합계가_들어간다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        long baselineJanuary = paymentRepository.groupByMonthStatisticConditions("PAID", from, to, null, null)
                .stream().filter(b -> "2026-01".equals(b.getBucketMonth()))
                .mapToLong(PaymentRepository.MonthlyAmountBucket::getBucketAmount).sum();

        var buckets = paymentRepository.groupByMonthStatisticConditions("PAID", from, to, null, null);
        var january = buckets.stream().filter(b -> "2026-01".equals(b.getBucketMonth())).findFirst().orElseThrow();

        assertThat(january.getBucketAmount()).isEqualTo(baselineJanuary);
        assertThat(january.getBucketAmount()).isGreaterThanOrEqualTo(3000L);
    }

    // 마이페이지 등급 달성률(PR #48 코드리뷰 대응 §3.3) — 회원 ID + 산정 시작일 기준 PAID 합계.
    @Test
    void sumAmountByMemberAndStatusSince_산정_시작일_이후_해당_회원의_PAID_금액만_합산한다() {
        Long gradeMemberId = insertMember("payment_grade_user", LocalDate.of(2000, 1, 1));
        Long gradeRentalId = insertRental(gradeMemberId, insertBook());
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 1, 0, 0);

        insertPayment(gradeMemberId, gradeRentalId, 5000, startAt.minusDays(1));
        insertPayment(gradeMemberId, gradeRentalId, 7000, startAt);
        insertPayment(gradeMemberId, gradeRentalId, 3000, startAt.plusDays(10));

        Long otherMemberId = insertMember("payment_grade_other", LocalDate.of(2000, 1, 1));
        Long otherRentalId = insertRental(otherMemberId, insertBook());
        insertPayment(otherMemberId, otherRentalId, 9000, startAt.plusDays(1));

        entityManager.flush();
        entityManager.clear();

        long sum = paymentRepository.sumAmountByMemberAndStatusSince(gradeMemberId, "PAID", startAt);

        assertThat(sum).isEqualTo(10000L);
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

    private Long insertBook() {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (title, rental_type, rental_price, default_rental_days, total_pages, " +
                        "rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES ('결제 통계 테스트 도서', 'PAID', 3000, 7, 100, 0, 0.00, 0, 'AVAILABLE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book");
    }

    private Long insertRental(Long memberId, Long bookId) {
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

    private void insertPayment(Long memberId, Long rentalId, int amount, LocalDateTime paidAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO payment (member_id, rental_id, payment_type, original_amount, amount, " +
                        "payment_method, payment_status, paid_at, created_at) " +
                        "VALUES (?, ?, 'RENTAL', ?, ?, 'CARD', 'PAID', ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, rentalId)
                .setParameter(3, amount)
                .setParameter(4, amount)
                .setParameter(5, paidAt)
                .setParameter(6, paidAt)
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
