package com.acorn.abc.batch.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorNameParserTest {

    @Test
    void parse_역할_표기가_있으면_제거하고_분리한다() {
        List<String> names = AuthorNameParser.parse("김영한 (지은이), 이영한 (옮긴이)");

        assertThat(names).containsExactly("김영한", "이영한");
    }

    @Test
    void parse_역할_표기가_없어도_그대로_분리한다() {
        List<String> names = AuthorNameParser.parse("김영한, 이영한");

        assertThat(names).containsExactly("김영한", "이영한");
    }

    @Test
    void parse_null이거나_빈값이면_빈_리스트를_반환한다() {
        assertThat(AuthorNameParser.parse(null)).isEmpty();
        assertThat(AuthorNameParser.parse("  ")).isEmpty();
    }

    @Test
    void parse_중복된_이름은_한번만_반환한다() {
        List<String> names = AuthorNameParser.parse("김영한 (지은이), 김영한 (엮은이)");

        assertThat(names).containsExactly("김영한");
    }

    @Test
    void parse_세미콜론과_지음_옮김_접미사를_제거한다() {
        List<String> names = AuthorNameParser.parse("로버트 C. 마틴 지음 ;이해영 옮김");

        assertThat(names).containsExactly("로버트 C. 마틴", "이해영");
    }

    @Test
    void parse_슬래시_구분자도_분리한다() {
        List<String> names = AuthorNameParser.parse("홍길동 저/이몽룡 역");

        assertThat(names).containsExactly("홍길동", "이몽룡");
    }

    @Test
    void parse_이름_끝음절이_접미사_단어와_같아도_공백이_없으면_잘리지_않는다() {
        List<String> names = AuthorNameParser.parse("마틴 (지은이), 김번역 (옮긴이)");

        assertThat(names).containsExactly("마틴", "김번역");
    }

    @Test
    void parse_앞쪽_역할_prefix를_세미콜론_구분자와_함께_제거한다() {
        List<String> names = AuthorNameParser.parse("글: 조영선 ;만화: 이영호");

        assertThat(names).containsExactly("조영선", "이영호");
    }

    @Test
    void parse_글그림_prefix와_쉼표_구분자를_함께_제거한다() {
        List<String> names = AuthorNameParser.parse("글·그림: 신태훈,나승훈");

        assertThat(names).containsExactly("신태훈", "나승훈");
    }
}
