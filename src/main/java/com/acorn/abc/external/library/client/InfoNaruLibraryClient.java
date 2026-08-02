package com.acorn.abc.external.library.client;

import com.acorn.abc.external.book.config.BookApiProperties;
import com.acorn.abc.external.library.dto.InfoNaruLibraryItem;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 정보나루 종이책 소장 도서관 조회 API 단건 조회.
 * GET /api/libSrchByBook?authKey=&isbn=&region=&dtl_region=&pageNo=&pageSize=&format=json
 * 응답: response.libs[].lib.{libCode,libName,address,latitude,longitude,homepage,closed,operationTime}
 * 내부 page 0은 외부 pageNo 1로 변환한다(계획 5.2).
 */
@Slf4j
@Component
public class InfoNaruLibraryClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration WARN_LATENCY = Duration.ofSeconds(2);

    private final RestClient infoNaruRestClient;
    private final BookApiProperties properties;

    public InfoNaruLibraryClient(@Qualifier("infoNaruRestClient") RestClient infoNaruRestClient,
                                  BookApiProperties properties) {
        this.infoNaruRestClient = infoNaruRestClient;
        this.properties = properties;
    }

    public InfoNaruLibrarySearchResult search(String isbn, String region, String dtlRegion,
                                               int page, int size) {
        if (!properties.isEnabled()) {
            return InfoNaruLibrarySearchResult.notTried();
        }
        String authKey = properties.getInfonaru().getKey();
        if (authKey == null || authKey.isBlank()) {
            return InfoNaruLibrarySearchResult.notTried();
        }

        int pageNo = page + 1;
        Instant startedAt = Instant.now();
        try {
            String body = infoNaruRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/libSrchByBook")
                                .queryParam("authKey", authKey)
                                .queryParam("isbn", isbn)
                                .queryParam("region", region)
                                .queryParam("pageNo", pageNo)
                                .queryParam("pageSize", size)
                                .queryParam("format", "json");
                        if (dtlRegion != null && !dtlRegion.isBlank()) {
                            uriBuilder.queryParam("dtl_region", dtlRegion);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);

            logIfSlow(startedAt, region, "SUCCESS_HTTP");

            if (body == null || body.isBlank()) {
                // 빈 body는 정상적인 "0건" 응답과 구분할 수 없는 비정상 상황이므로 실패로 처리한다.
                log.warn("정보나루 libSrchByBook 빈 응답 body, region={}", region);
                return InfoNaruLibrarySearchResult.failedRetryable();
            }
            return parse(body);
        } catch (Exception e) {
            logIfSlow(startedAt, region, "ERROR");
            // authKey, isbn 등 민감정보가 예외 메시지(요청 URI 포함 가능)에 담길 수 있어 message는 기록하지 않는다.
            log.warn("정보나루 libSrchByBook 호출 실패 region={}, exceptionType={}", region, e.getClass().getSimpleName());
            return InfoNaruLibrarySearchResult.failedRetryable();
        }
    }

    // numFound가 응답 판정의 기준이다: 0이면 정상 "0건"(SUCCESS_NO_DATA), 0보다 크면 성공(현재 page가
    // 비어 있어도 totalCount는 numFound를 유지한다). response/numFound/libs 중 하나라도 기대한 형태가
    // 아니면(malformed, error JSON 등) FAILED_RETRYABLE로 처리해 "0건"과 혼동하지 않는다.
    private InfoNaruLibrarySearchResult parse(String body) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            log.warn("정보나루 libSrchByBook 응답 파싱 실패, exceptionType={}", e.getClass().getSimpleName());
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        JsonNode response = root.path("response");
        if (response.isMissingNode() || response.isNull()) {
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        JsonNode numFoundNode = response.path("numFound");
        if (numFoundNode.isMissingNode() || numFoundNode.isNull()
                || !(numFoundNode.isIntegralNumber() || numFoundNode.isTextual())) {
            return InfoNaruLibrarySearchResult.failedRetryable();
        }
        int numFound;
        try {
            numFound = numFoundNode.isTextual() ? Integer.parseInt(numFoundNode.asText().trim()) : numFoundNode.asInt();
        } catch (NumberFormatException e) {
            return InfoNaruLibrarySearchResult.failedRetryable();
        }
        if (numFound < 0) {
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        // libs 형태 검증은 numFound 분기보다 먼저 한다: numFound==0이라고 바로 noData()를 반환하면
        // libs가 문자열/객체처럼 완전히 깨진 형태여도 검증 없이 "정상 0건"으로 둔갑한다. libs가
        // 존재하는데 array가 아니면(문자열 등) 응답 자체가 malformed이므로 numFound 값과 무관하게
        // 실패로 처리한다. libs가 아예 없는 것(누락)은 0건 응답에서 흔하므로 별도로만 검증한다.
        JsonNode libs = response.path("libs");
        boolean libsPresent = !libs.isMissingNode() && !libs.isNull();
        if (libsPresent && !libs.isArray()) {
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        if (numFound == 0) {
            // numFound=0인데 libs가 array로 존재하면서 실제 항목을 담고 있으면 응답 자체가 모순이다
            // (0건이라면서 도서관 항목이 있음). numFound 쪽이 오히려 잘못됐을 수 있으므로 이 값을
            // 신뢰해 SUCCESS_NO_DATA로 cache하지 않고 malformed로 처리해 fallback을 타게 한다.
            if (libsPresent && libs.size() > 0) {
                return InfoNaruLibrarySearchResult.failedRetryable();
            }
            return InfoNaruLibrarySearchResult.noData();
        }

        if (!libsPresent) {
            // numFound>0인데 libs 자체가 없으면 응답이 서로 모순되는 것이므로 malformed로 처리한다.
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        List<InfoNaruLibraryItem> items = new ArrayList<>();
        for (JsonNode libWrapper : libs) {
            JsonNode lib = libWrapper.path("lib");
            if (lib.isMissingNode() || lib.isNull()) {
                continue;
            }
            String libraryCode = textOrNull(lib, "libCode");
            String libraryName = textOrNull(lib, "libName");
            String address = textOrNull(lib, "address");
            if (libraryCode == null || libraryName == null) {
                continue;
            }
            items.add(new InfoNaruLibraryItem(
                    libraryCode,
                    libraryName,
                    address,
                    decimalOrNull(lib, "latitude"),
                    decimalOrNull(lib, "longitude"),
                    textOrNull(lib, "homepage"),
                    textOrNull(lib, "closed"),
                    textOrNull(lib, "operationTime")
            ));
        }

        // libs 배열 자체는 비어있지 않은데(현재 page에 항목이 있다는 뜻) 파싱 가능한 항목이 하나도
        // 없다면 이는 "정상 0건"이 아니라 응답 형식이 기대와 달라 전부 걸러진 것이다. 이 경우를
        // SUCCESS_HAS_DATA(빈 목록)로 그대로 두면 잘못된 결과가 cache에 성공으로 저장되므로
        // FAILED_RETRYABLE로 처리해 fallback 경로를 타게 한다. libs가 원래 빈 배열인 경우(현재 page가
        // numFound 범위 밖이라 비어 있는 정상 상황)는 이 분기에 들어오지 않는다.
        if (!libs.isEmpty() && items.isEmpty()) {
            log.warn("정보나루 libSrchByBook 모든 lib 항목이 malformed, numFound={}", numFound);
            return InfoNaruLibrarySearchResult.failedRetryable();
        }

        return InfoNaruLibrarySearchResult.success(items, numFound);
    }

    private void logIfSlow(Instant startedAt, String region, String resultStatus) {
        Duration elapsed = Duration.between(startedAt, Instant.now());
        if (elapsed.compareTo(WARN_LATENCY) > 0) {
            log.warn("정보나루 libSrchByBook 응답 지연 provider=INFONARU_LIBRARY, region={}, latencyMs={}, result={}",
                    region, elapsed.toMillis(), resultStatus);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
