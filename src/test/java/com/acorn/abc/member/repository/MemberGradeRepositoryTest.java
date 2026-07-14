package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.MemberGrade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이페이지 등급 달성률(PR #48 코드리뷰 대응 §3.3) — findByGradeLevel 검증.
 * member_grade는 data.sql로 고정 시드(grade_id 1~4, grade_level 1~4, grade_id 2의 min_payment_amount=20000)되는
 * 참조 데이터라 절대값으로 검증한다. grade_name(한글) 비교는 이 로컬 환경의 data.sql 로딩 인코딩(mojibake) 이슈로
 * 제외하고 인코딩 영향이 없는 grade_id/min_payment_amount만 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberGradeRepositoryTest {

    @Autowired
    private MemberGradeRepository memberGradeRepository;

    @Test
    void findByGradeLevel_현재_등급보다_한_단계_높은_다음_등급을_조회한다() {
        Optional<MemberGrade> nextGrade = memberGradeRepository.findByGradeLevel(2);

        assertThat(nextGrade).isPresent();
        assertThat(nextGrade.get().getGradeId()).isEqualTo(2L);
        assertThat(nextGrade.get().getGradeLevel()).isEqualTo(2);
        assertThat(nextGrade.get().getMinPaymentAmount()).isEqualTo(20000);
    }

    @Test
    void findByGradeLevel_최고_등급보다_높은_등급이_없으면_빈_결과를_반환한다() {
        Optional<MemberGrade> nextGrade = memberGradeRepository.findByGradeLevel(5);

        assertThat(nextGrade).isEmpty();
    }
}
