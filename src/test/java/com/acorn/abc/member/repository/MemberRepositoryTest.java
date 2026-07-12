package com.acorn.abc.member.repository;

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
 * 관리자 통계 확장(PR #42 리뷰 Watchlist W2) — countByAgeBand/countByStatusAndAgeBand 나이대 경계 검증.
 * 실제 공유 MySQL DB를 사용하므로 절대 건수 대신 "삽입 전/후 차이"로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 7, 12);
    private static final LocalDate BIRTH_DATE_FROM = BASE_DATE.minusYears(30);
    private static final LocalDate BIRTH_DATE_TO = BASE_DATE.minusYears(20);

    private long baselineAgeBandCount;
    private long baselineSanctionedAgeBandCount;

    @BeforeEach
    void setUp() {
        baselineAgeBandCount = memberRepository.countByAgeBand(BIRTH_DATE_FROM, BIRTH_DATE_TO);
        baselineSanctionedAgeBandCount = memberRepository.countByStatusAndAgeBand("SANCTIONED", BIRTH_DATE_FROM, BIRTH_DATE_TO);

        // 20S 경계: birthDate <= baseDate-20y(2006-07-12) AND birthDate > baseDate-30y(1996-07-12)
        insertMember("age_boundary_1", LocalDate.of(2006, 7, 12), "JOINED"); // 만 20세, 경계 포함(<=)
        insertMember("age_boundary_2", LocalDate.of(2006, 7, 13), "JOINED"); // 만 19세, 경계 제외
        insertMember("age_boundary_3", LocalDate.of(1996, 7, 12), "JOINED"); // 만 30세, 경계 제외(>)
        insertMember("age_boundary_4", LocalDate.of(2000, 1, 1), "SANCTIONED"); // 20S, 제재 회원

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void countByAgeBand_null_필터는_전체_회원수를_반환한다() {
        long count = memberRepository.countByAgeBand(null, null);

        assertThat(count).isGreaterThanOrEqualTo(4);
    }

    @Test
    void countByAgeBand_생일_당일은_해당_나이대에_포함된다() {
        long count = memberRepository.countByAgeBand(BIRTH_DATE_FROM, BIRTH_DATE_TO);

        // age_boundary_1(2006-07-12, 경계 포함), age_boundary_4(2000-01-01) -> 기준선 + 2명
        // age_boundary_2(2006-07-13, 만 19세)와 age_boundary_3(1996-07-12, 경계 제외)은 미포함
        assertThat(count).isEqualTo(baselineAgeBandCount + 2);
    }

    @Test
    void countByStatusAndAgeBand_상태와_나이대_조건을_함께_적용한다() {
        long count = memberRepository.countByStatusAndAgeBand("SANCTIONED", BIRTH_DATE_FROM, BIRTH_DATE_TO);

        assertThat(count).isEqualTo(baselineSanctionedAgeBandCount + 1);
    }

    private void insertMember(String loginId, LocalDate birthDate, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', 'Tester', ?, ?, 'USER', 1, 0, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId + "@test.com")
                .setParameter(3, birthDate)
                .setParameter(4, status)
                .setParameter(5, LocalDateTime.now())
                .executeUpdate();
    }
}
