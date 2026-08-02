package com.acorn.abc.member.repository;

import com.acorn.abc.common.config.JpaAuditingConfig;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR49 코드리뷰 후속수정계획(2026-07-15) §8.3 — member.login_id의 실제 MySQL Collation
 * (utf8mb4_0900_ai_ci, schema.sql 기준) 대소문자 무시 중복 판정을 Mock이 아닌 실제 DB로 고정한다.
 * @DataJpaTest는 기본적으로 각 테스트를 트랜잭션 롤백하므로 임시 회원은 테스트 종료 후 남지 않는다
 * (BookRepositoryTest/MemberGradeRepositoryTest와 동일한 기존 프로젝트 패턴).
 * @DataJpaTest 슬라이스는 JpaAuditingConfig를 자동으로 로드하지 않아 BaseEntity.createdAt이
 * null로 남는다 — Member.create()+save()로 실제 엔티티 라이프사이클을 태우려면 명시적 @Import가 필요하다.
 * 다른 팀원/테스트와 충돌하지 않도록 통상적인 회원가입 아이디로 쓰이지 않을 접두사를 사용한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class MemberRepositoryLoginIdIntegrationTest {

    private static final String TEST_LOGIN_ID = "pr49CaseTestXyz";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberGradeRepository memberGradeRepository;

    @BeforeEach
    void setUp() {
        MemberGrade defaultGrade = memberGradeRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("data.sql의 grade_id=1 시드가 필요합니다."));

        Member member = Member.create(
                TEST_LOGIN_ID,
                "hashed-password",
                "PR49대소문자테스트",
                "pr49-case-test@example.com",
                null,
                LocalDate.of(1995, 1, 1),
                null,
                defaultGrade);

        memberRepository.save(member);
        memberRepository.flush();
    }

    @Test
    void existsByLoginId_원본_대소문자는_true를_반환한다() {
        assertThat(memberRepository.existsByLoginId(TEST_LOGIN_ID)).isTrue();
    }

    @Test
    void existsByLoginId_전체_대문자로_바꾼_값도_true를_반환한다() {
        assertThat(memberRepository.existsByLoginId(TEST_LOGIN_ID.toUpperCase())).isTrue();
    }

    @Test
    void existsByLoginId_전체_소문자로_바꾼_값도_true를_반환한다() {
        assertThat(memberRepository.existsByLoginId(TEST_LOGIN_ID.toLowerCase())).isTrue();
    }

    @Test
    void existsByLoginId_존재하지_않는_아이디는_false를_반환한다() {
        assertThat(memberRepository.existsByLoginId("pr49NotExistXyz")).isFalse();
    }
}
