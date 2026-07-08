package com.acorn.abc.challenge.repository;

import com.acorn.abc.challenge.domain.ChallengeProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChallengeProgressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChallengeProgressRepository challengeProgressRepository;

    private Long memberId;
    private Long dailyChallengeId;
    private Long totalChallengeId;
    private Long dailyChallengeWithoutProgressId;
    private Long totalChallengeWithoutProgressId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("challenge_test_user");
        dailyChallengeId = insertChallenge("일일 출석", "DAILY", "ATTENDANCE", 1, "ACTIVE");
        totalChallengeId = insertChallenge("누적 출석 100일", "TOTAL", "ATTENDANCE", 100, "ACTIVE");
        dailyChallengeWithoutProgressId = insertChallenge("일일 출석(진행도 없음)", "DAILY", "ATTENDANCE", 1, "ACTIVE");
        totalChallengeWithoutProgressId = insertChallenge("누적 출석(진행도 없음)", "TOTAL", "ATTENDANCE", 50, "ACTIVE");

        insertProgress(memberId, dailyChallengeId, LocalDate.now(), 1, true);
        insertProgress(memberId, totalChallengeId, LocalDate.now().minusDays(5), 10, false);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByMemberAndChallengeAndDateReturnsDailyProgress() {
        Optional<ChallengeProgress> result = challengeProgressRepository
                .findByMember_MemberIdAndChallenge_ChallengeIdAndProgressDate(memberId, dailyChallengeId, LocalDate.now());

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentCount()).isEqualTo(1);
        assertThat(result.get().getAchievedYn()).isTrue();
    }

    @Test
    void findFirstByMemberAndChallengeReturnsTotalProgressRegardlessOfDate() {
        Optional<ChallengeProgress> result = challengeProgressRepository
                .findFirstByMember_MemberIdAndChallenge_ChallengeId(memberId, totalChallengeId);

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentCount()).isEqualTo(10);
        assertThat(result.get().getAchievedYn()).isFalse();
    }

    @Test
    void findByChallengeIdsInAndProgressDateReturnsOnlyDailyProgressForToday() {
        List<ChallengeProgress> result = challengeProgressRepository
                .findByMember_MemberIdAndChallenge_ChallengeIdInAndProgressDate(
                        memberId, List.of(dailyChallengeId, dailyChallengeWithoutProgressId), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChallenge().getChallengeId()).isEqualTo(dailyChallengeId);
        assertThat(result.get(0).getCurrentCount()).isEqualTo(1);
    }

    @Test
    void findByChallengeIdsInReturnsOnlyExistingTotalProgress() {
        List<ChallengeProgress> result = challengeProgressRepository
                .findByMember_MemberIdAndChallenge_ChallengeIdIn(
                        memberId, List.of(totalChallengeId, totalChallengeWithoutProgressId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChallenge().getChallengeId()).isEqualTo(totalChallengeId);
        assertThat(result.get(0).getCurrentCount()).isEqualTo(10);
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

    private Long insertChallenge(String name, String type, String targetAction, int targetCount, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO challenge (challenge_name, challenge_type, target_action, target_count, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, name)
                .setParameter(2, type)
                .setParameter(3, targetAction)
                .setParameter(4, targetCount)
                .setParameter(5, status)
                .setParameter(6, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("challenge").longValue();
    }

    private void insertProgress(Long memberId, Long challengeId, LocalDate date, int currentCount, boolean achievedYn) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO challenge_progress (member_id, challenge_id, progress_date, current_count, achieved_yn) " +
                        "VALUES (?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, challengeId)
                .setParameter(3, date)
                .setParameter(4, currentCount)
                .setParameter(5, achievedYn)
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
