package com.acorn.abc.coupon.repository;

import com.acorn.abc.payment.domain.MemberCoupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberCouponRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    private Long memberId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("coupon_test_user");
        couponId = insertCoupon("5% 할인 쿠폰", "PERCENT_DISCOUNT", 5, "PERCENT", 14, "ACTIVE");

        insertMemberCoupon(memberId, couponId, "ISSUED");
        insertMemberCoupon(memberId, couponId, "USED");
        insertMemberCoupon(memberId, couponId, "EXPIRED");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByMemberIdReturnsAllCoupons() {
        Page<MemberCoupon> page = memberCouponRepository.findByMember_MemberId(memberId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByMemberIdAndStatusFiltersByStatus() {
        Page<MemberCoupon> page = memberCouponRepository.findByMember_MemberIdAndCouponStatus(memberId, "ISSUED", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void countByCouponIdReturnsTotalIssuedRegardlessOfStatus() {
        long count = memberCouponRepository.countByCoupon_CouponId(couponId);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByCouponIdAndStatusFiltersCorrectly() {
        long usedCount = memberCouponRepository.countByCoupon_CouponIdAndCouponStatus(couponId, "USED");
        assertThat(usedCount).isEqualTo(1);
    }

    private Long insertMember(String loginId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', 'Tester', ?, '2000-01-01', 'USER', 1, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId + "@test.com")
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member").longValue();
    }

    private Long insertCoupon(String name, String type, int benefitValue, String benefitUnit, int validDays, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO coupon (coupon_name, coupon_type, benefit_value, benefit_unit, valid_days, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, type)
                .setParameter(3, benefitValue)
                .setParameter(4, benefitUnit)
                .setParameter(5, validDays)
                .setParameter(6, status)
                .setParameter(7, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("coupon").longValue();
    }

    private void insertMemberCoupon(Long memberId, Long couponId, String status) {
        LocalDateTime now = LocalDateTime.now();
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member_coupon (member_id, coupon_id, coupon_status, issued_at, expires_at) VALUES (?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, couponId)
                .setParameter(3, status)
                .setParameter(4, now)
                .setParameter(5, now.plusDays(14))
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
