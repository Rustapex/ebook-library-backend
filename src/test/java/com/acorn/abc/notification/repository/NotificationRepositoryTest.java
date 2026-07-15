package com.acorn.abc.notification.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이페이지 미읽은 알림 수(PR #48 코드리뷰 대응 §3.2) — countByMember_MemberIdAndReadYnFalse 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("notification_test_user");
        Long otherMemberId = insertMember("notification_test_other");

        insertNotification(memberId, false);
        insertNotification(memberId, false);
        insertNotification(memberId, true);
        insertNotification(otherMemberId, false);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void countByMember_MemberIdAndReadYnFalse_읽은_알림과_다른_회원_알림을_제외한다() {
        long count = notificationRepository.countByMember_MemberIdAndReadYnFalse(memberId);

        assertThat(count).isEqualTo(2L);
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
        return lastInsertId("member");
    }

    private void insertNotification(Long memberId, boolean readYn) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO notification (member_id, notification_type, title, read_yn, created_at) " +
                        "VALUES (?, 'RENTAL', '테스트 알림', ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, readYn)
                .setParameter(3, LocalDateTime.now())
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
