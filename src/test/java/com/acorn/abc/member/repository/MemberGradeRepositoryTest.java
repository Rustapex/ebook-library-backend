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
 * 참조 데이터라 절대값으로 검증한다.
 * grade_name(한글) 비교는 회원정보수정 보완계획(2026-07-15) §6.3 대상이다 — spring.sql.init.encoding
 * 미지정으로 data.sql의 한글 리터럴이 깨져 저장되는 mojibake가 있었고, spring.sql.init.encoding=UTF-8
 * 추가로 다음 부팅부터 ON DUPLICATE KEY UPDATE가 정상 값으로 덮어쓴다. 이 테스트가 실패하면 인코딩
 * 설정이 아직 적용되지 않았거나 이 DB가 spring.sql.init.mode=always로 재기동되지 않은 것이다.
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
        assertThat(nextGrade.get().getGradeName()).isEqualTo("새싹");
    }

    @Test
    void findByGradeLevel_기본_등급_씨앗을_정상_한글로_조회한다() {
        Optional<MemberGrade> baseGrade = memberGradeRepository.findByGradeLevel(1);

        assertThat(baseGrade).isPresent();
        assertThat(baseGrade.get().getGradeName()).isEqualTo("씨앗");
    }

    @Test
    void findByGradeLevel_최고_등급보다_높은_등급이_없으면_빈_결과를_반환한다() {
        Optional<MemberGrade> nextGrade = memberGradeRepository.findByGradeLevel(5);

        assertThat(nextGrade).isEmpty();
    }
}
