package com.acorn.abc.batch.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 알라딘/정보나루 author 문자열(예: "로버트 C. 마틴 지음 ;이해영 옮김", "김영한 (지은이), 이영한 (옮긴이)",
 * "글: 조영선 ;만화: 이영호", "글·그림: 신태훈,나승훈")을 이름 목록으로 분리한다.
 * 처리 순서: 1) ,;/ 로 저자 분리 2) 앞쪽 역할 prefix(글:, 그림: 등) 제거
 * 3) 괄호 역할 표기((지은이) 등) 제거 4) 접미 역할 표기(지음/옮김/저/역 등) 제거 5) trim/빈값/중복 제거.
 */
public final class AuthorNameParser {

    private static final Pattern SEPARATOR = Pattern.compile("[,;/]");
    private static final Pattern PREFIX_ROLE =
            Pattern.compile("^\\s*(글·그림|지은이|옮긴이|저자|그림|만화|글|역자)\\s*:\\s*");
    private static final Pattern PAREN_ROLE = Pattern.compile("\\([^)]*\\)");
    private static final Pattern SUFFIX_ROLE = Pattern.compile("\\s+(지음|옮김|엮음|그림|저|역)\\s*$");

    private AuthorNameParser() {
    }

    public static List<String> parse(String rawAuthor) {
        if (rawAuthor == null || rawAuthor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(SEPARATOR.split(rawAuthor))
                .map(AuthorNameParser::cleanName)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
    }

    private static String cleanName(String token) {
        String name = PREFIX_ROLE.matcher(token).replaceFirst("");
        name = PAREN_ROLE.matcher(name).replaceAll("").trim();
        String previous;
        do {
            previous = name;
            name = SUFFIX_ROLE.matcher(name).replaceAll("").trim();
        } while (!name.equals(previous));
        return name;
    }
}
