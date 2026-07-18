package com.acorn.abc.review.repository;

import com.acorn.abc.review.domain.SanctionHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 회원 API 구현계획(2026-07-16) §1.5 — 만료 배치/현재 유효 제재 조회 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SanctionHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SanctionHistoryRepository sanctionHistoryRepository;

    private Long userMemberId;
    private Long adminMemberId;

    @BeforeEach
    void setUp() {
        userMemberId = insertMember("sanction_test_user", "USER");
        adminMemberId = insertMember("sanction_test_admin", "ADMIN");
    }

    @Test
    void findExpiredAccountSuspensionCandidates_만료된_ACCOUNT_SUSPENSION만_ADMIN을_제외하고_반환한다() {
        Long expired = insertSanction(userMemberId, "ACCOUNT_SUSPENSION",
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        insertSanction(userMemberId, "ACCOUNT_SUSPENSION", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(5));
        insertSanction(userMemberId, "RENTAL_BAN", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        insertSanction(adminMemberId, "ACCOUNT_SUSPENSION", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        entityManager.flush();
        entityManager.clear();

        List<SanctionHistory> candidates = sanctionHistoryRepository.findExpiredAccountSuspensionCandidates(LocalDateTime.now());

        assertThat(candidates).extracting(SanctionHistory::getSanctionHistoryId).containsExactly(expired);
    }

    @Test
    void findActiveAccountSuspensionsByMember_현재_유효한_ACCOUNT_SUSPENSION만_최신순으로_반환한다() {
        insertSanction(userMemberId, "ACCOUNT_SUSPENSION", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        Long olderActive = insertSanction(userMemberId, "ACCOUNT_SUSPENSION",
                LocalDateTime.now().minusDays(3), LocalDateTime.now().plusDays(1));
        Long newerActive = insertSanction(userMemberId, "ACCOUNT_SUSPENSION",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(3));
        entityManager.flush();
        entityManager.clear();

        List<SanctionHistory> active = sanctionHistoryRepository
                .findActiveAccountSuspensionsByMember(userMemberId, LocalDateTime.now());

        assertThat(active).extracting(SanctionHistory::getSanctionHistoryId).containsExactly(newerActive, olderActive);
    }

    @Test
    void findActiveSanctionSummariesByMemberIds_현재_유효기간_안의_제재만_반환한다() {
        insertSanction(userMemberId, "RENTAL_BAN", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        insertSanction(userMemberId, "REVIEW_BAN", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(5));
        entityManager.flush();
        entityManager.clear();

        List<SanctionHistoryRepository.ActiveSanctionSummaryView> summaries = sanctionHistoryRepository
                .findActiveSanctionSummariesByMemberIds(List.of(userMemberId), LocalDateTime.now());

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getSanctionType()).isEqualTo("RENTAL_BAN");
    }

    @Test
    void findByMemberIdForAdmin_startedAt_내림차순_id_내림차순으로_페이지를_반환한다() {
        Long first = insertSanction(userMemberId, "RENTAL_BAN", LocalDateTime.now().minusDays(3), null);
        Long second = insertSanction(userMemberId, "REVIEW_BAN", LocalDateTime.now().minusDays(1), null);
        entityManager.flush();
        entityManager.clear();

        Page<SanctionHistory> page = sanctionHistoryRepository.findByMemberIdForAdmin(userMemberId, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(SanctionHistory::getSanctionHistoryId).containsExactly(second, first);
    }

    private Long insertMember(String loginId, String role) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', 'Tester', ?, '2000-01-01', ?, 1, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId + "@test.com")
                .setParameter(3, role)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member");
    }

    private Long insertSanction(Long memberId, String sanctionType, LocalDateTime startedAt, LocalDateTime endedAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO sanction_history (member_id, sanction_type, started_at, ended_at, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, sanctionType)
                .setParameter(3, startedAt)
                .setParameter(4, endedAt)
                .setParameter(5, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("sanction_history");
    }

    private Long lastInsertId(String table) {
        String idColumn = table + "_id";
        return ((Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult()).longValue();
    }
}
