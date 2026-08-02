package com.acorn.abc.external.book.client;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.external.book.config.BookApiProperties;
import com.acorn.abc.external.book.dto.ExternalBookSeedCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 정보나루 도서 목록 검색 API 조회 (ISBN 후보 대량 수집용).
 * GET /api/srchBooks?authKey=&keyword=&pageNo=&pageSize=&format=json
 * 응답: response.docs[].doc.{isbn13,bookname,publisher}
 *
 * 비활성화/키 누락(설정 문제)과 호출·파싱 실패(provider 오류)는 예외로 즉시 실패 처리하고,
 * 정상 응답에 docs가 없는 경우만 "검색 결과 0건"으로 간주해 빈 리스트를 반환한다.
 * 이 셋을 구분하지 않으면 설정 문제로 수집이 안 되는 상황을 빈 검색 결과와 구별할 수 없다.
 */
@Slf4j
@Component
public class InfoNaruBookListClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern ISBN13_PATTERN = Pattern.compile("\\d{13}");

    private final RestClient infoNaruRestClient;
    private final BookApiProperties properties;

    public InfoNaruBookListClient(@Qualifier("infoNaruRestClient") RestClient infoNaruRestClient,
                                   BookApiProperties properties) {
        this.infoNaruRestClient = infoNaruRestClient;
        this.properties = properties;
    }

    public List<ExternalBookSeedCandidate> search(String keyword, int pageNo, int pageSize) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.EXTERNAL_BOOK_API_DISABLED);
        }
        String authKey = properties.getInfonaru().getKey();
        if (authKey == null || authKey.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_BOOK_API_DISABLED);
        }

        String body;
        try {
            body = infoNaruRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/srchBooks")
                            .queryParam("authKey", authKey)
                            .queryParam("keyword", keyword)
                            .queryParam("pageNo", pageNo)
                            .queryParam("pageSize", pageSize)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("정보나루 목록 API 호출 실패 keyword={}, pageNo={}, message={}", keyword, pageNo, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_BOOK_API_CALL_FAILED);
        }

        if (body == null || body.isBlank()) {
            return List.of();
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            log.warn("정보나루 목록 API 응답 파싱 실패 keyword={}, pageNo={}, message={}", keyword, pageNo, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_BOOK_API_CALL_FAILED);
        }

        JsonNode docs = root.path("response").path("docs");
        if (!docs.isArray() || docs.isEmpty()) {
            return List.of();
        }

        List<ExternalBookSeedCandidate> candidates = new ArrayList<>();
        for (JsonNode docWrapper : docs) {
            JsonNode doc = docWrapper.path("doc");
            if (doc.isMissingNode() || doc.isNull()) {
                continue;
            }
            String isbn13 = textOrNull(doc, "isbn13");
            if (isbn13 == null || !ISBN13_PATTERN.matcher(isbn13).matches()) {
                continue;
            }
            String title = textOrNull(doc, "bookname");
            String publisher = textOrNull(doc, "publisher");
            if (title == null || publisher == null) {
                continue;
            }
            candidates.add(new ExternalBookSeedCandidate(isbn13, title, publisher));
        }
        return candidates;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
