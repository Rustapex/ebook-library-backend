package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.PointHistory;
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
class PointHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("point_test_user");
        // NOTE: point_history.point_type CHECK 제약이 'challenge_reward'(소문자)로 되어있는 기존 버그(계획 문서 3-1)가
        // 아직 수정되지 않아, 이 테스트는 이미 대문자로 맞게 들어있는 REVIEW_REWARD/ADMIN_ADJUST만 사용한다.
        insertPointHistory(memberId, "REVIEW_REWARD", 100);
        insertPointHistory(memberId, "ADMIN_ADJUST", -50);
        insertPointHistory(memberId, "REVIEW_REWARD", 200);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByMemberIdReturnsAllHistoryForMember() {
        Page<PointHistory> page = pointHistoryRepository.findByMember_MemberId(memberId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByMemberIdAndPointTypeFiltersByType() {
        Page<PointHistory> page = pointHistoryRepository.findByMember_MemberIdAndPointType(memberId, "REVIEW_REWARD", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(ph -> "REVIEW_REWARD".equals(ph.getPointType()));
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

    private void insertPointHistory(Long memberId, String pointType, int amount) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO point_history (member_id, point_type, point_amount, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, pointType)
                .setParameter(3, amount)
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
