package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Attendance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AttendanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("attend_test_user");
        insertAttendance(memberId, LocalDate.now().minusDays(2));
        insertAttendance(memberId, LocalDate.now().minusDays(1));
        insertAttendance(memberId, LocalDate.now());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByMemberAndDateDetectsAlreadyCheckedInToday() {
        boolean exists = attendanceRepository.existsByMember_MemberIdAndAttendanceDate(memberId, LocalDate.now());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByMemberAndDateReturnsFalseForFutureDate() {
        boolean exists = attendanceRepository.existsByMember_MemberIdAndAttendanceDate(memberId, LocalDate.now().plusDays(1));
        assertThat(exists).isFalse();
    }

    @Test
    void findByMemberOrderByAttendanceDateDescReturnsMostRecentFirst() {
        List<Attendance> result = attendanceRepository.findByMember_MemberIdOrderByAttendanceDateDesc(memberId);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAttendanceDate()).isEqualTo(LocalDate.now());
        assertThat(result.get(2).getAttendanceDate()).isEqualTo(LocalDate.now().minusDays(2));
    }

    @Test
    void findByMemberAndDateBetweenReturnsAscendingOrder() {
        List<Attendance> result = attendanceRepository.findByMember_MemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                memberId, LocalDate.now().minusDays(2), LocalDate.now());

        assertThat(result).extracting(Attendance::getAttendanceDate)
                .containsExactly(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), LocalDate.now());
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

    private void insertAttendance(Long memberId, LocalDate date) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO attendance (member_id, attendance_date, created_at) VALUES (?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, date)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
    }

    private Number lastInsertId(String table) {
        String idColumn = table + "_id";
        return (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
    }
}
