package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // 관리자 회원 API 구현계획(2026-07-16) §2.1 — point_history UNIQUE(member_id, request_id) 검증.
    // TestEntityManager.persistAndFlush()는 EntityManager를 직접 호출해 Spring의 예외 변환(@Repository
    // 프록시)을 거치지 않으므로 raw ConstraintViolationException이 나온다. 실제 서비스 코드
    // (AdminPointAdjustExecutor)는 pointHistoryRepository.save()를 거치므로 repository를 통해
    // 동일 경로로 검증한다.
    @Test
    void 같은_회원의_같은_requestId는_UNIQUE_제약을_위반한다() {
        String requestId = UUID.randomUUID().toString();
        Member member = entityManager.find(Member.class, memberId);
        pointHistoryRepository.save(PointHistory.createAdminAdjust(member, 100, "관리자 지급", requestId, 1100));
        pointHistoryRepository.flush();

        assertThatThrownBy(() -> {
            pointHistoryRepository.save(PointHistory.createAdminAdjust(member, 200, "관리자 지급2", requestId, 1300));
            pointHistoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // PR51 코드리뷰 대응(2026-07-18) §2.2 — balanceAfter가 조회에서도 그대로 유지되는지 확인한다.
    @Test
    void findByMember_MemberIdAndRequestId_요청ID로_이력을_조회하고_balanceAfter를_포함한다() {
        String requestId = UUID.randomUUID().toString();
        Member member = entityManager.find(Member.class, memberId);
        entityManager.persistAndFlush(PointHistory.createAdminAdjust(member, 100, "관리자 지급", requestId, 1100));
        entityManager.clear();

        Optional<PointHistory> found = pointHistoryRepository.findByMember_MemberIdAndRequestId(memberId, requestId);

        assertThat(found).isPresent();
        assertThat(found.get().getPointAmount()).isEqualTo(100);
        assertThat(found.get().getBalanceAfter()).isEqualTo(1100);
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
