package com.acorn.abc.external.library.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.acorn.abc.external.book.config.BookApiProperties;
import com.acorn.abc.external.library.dto.InfoNaruLibraryFetchStatus;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InfoNaruLibraryClientTest {

    private static final String AUTH_KEY = "super-secret-auth-key";
    private ListAppender<ILoggingEvent> logAppender;

    private BookApiProperties enabledProperties() {
        BookApiProperties properties = new BookApiProperties();
        properties.setEnabled(true);
        properties.getInfonaru().setKey(AUTH_KEY);
        return properties;
    }

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(InfoNaruLibraryClient.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(InfoNaruLibraryClient.class)).detachAppender(logAppender);
    }

    private String allLogMessages() {
        StringBuilder sb = new StringBuilder();
        logAppender.list.forEach(event -> sb.append(event.getFormattedMessage()).append('\n'));
        return sb.toString();
    }

    @Test
    void search_비활성화_상태면_NOT_TRIED() {
        BookApiProperties properties = enabledProperties();
        properties.setEnabled(false);
        InfoNaruLibraryClient client = new InfoNaruLibraryClient(
                RestClient.builder().baseUrl("http://data4library.kr").build(), properties);

        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.NOT_TRIED);
    }

    @Test
    void search_인증키가_없으면_NOT_TRIED() {
        BookApiProperties properties = enabledProperties();
        properties.getInfonaru().setKey("");
        InfoNaruLibraryClient client = new InfoNaruLibraryClient(
                RestClient.builder().baseUrl("http://data4library.kr").build(), properties);

        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.NOT_TRIED);
    }

    @Test
    void search_page0은_pageNo1로_변환된다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andExpect(queryParam("pageNo", "1"))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        client.search("9788966260959", "11", null, 0, 20);

        server.verify();
    }

    @Test
    void search_요청_query_parameter가_모두_올바르게_전달된다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andExpect(queryParam("isbn", "9788966260959"))
                .andExpect(queryParam("region", "11"))
                .andExpect(queryParam("dtl_region", "11110"))
                .andExpect(queryParam("pageNo", "4"))
                .andExpect(queryParam("pageSize", "30"))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("authKey", AUTH_KEY))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        client.search("9788966260959", "11", "11110", 3, 30);

        server.verify();
    }

    @Test
    void search_dtlRegion이_없으면_dtl_region_parameter를_보내지_않는다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("dtl_region"))))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        client.search("9788966260959", "11", null, 0, 20);

        server.verify();
    }

    @Test
    void search_numFound이_0이면_SUCCESS_NO_DATA() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.SUCCESS_NO_DATA);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void search_numFound이_0인데_libs에_실제_항목이_있으면_모순으로_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess(
                        "{\"response\":{\"numFound\":0,\"libs\":[{\"lib\":{\"libCode\":\"111108\","
                                + "\"libName\":\"강남도서관\"}}]}}",
                        MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        // numFound=0이라면서 libs에 실제 도서관 항목이 있는 것은 응답 자체가 모순이다. numFound을
        // 그대로 신뢰해 SUCCESS_NO_DATA로 cache하면 실제로는 존재하는 소장 정보를 "0건"으로
        // 잘못 캐싱하게 된다.
        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_numFound이_0이어도_libs가_배열이_아니면_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0,\"libs\":\"not-an-array\"}}",
                        MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_numFound이_0이고_libs가_아예_없으면_SUCCESS_NO_DATA() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":0}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.SUCCESS_NO_DATA);
    }

    @Test
    void search_numFound이_있으면_현재_page가_비어도_totalCount를_유지한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":5,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 3, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.SUCCESS_HAS_DATA);
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(5);
    }

    @Test
    void search_정상_응답을_item으로_변환한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess(
                        "{\"response\":{\"numFound\":1,\"libs\":[{\"lib\":{\"libCode\":\"111108\","
                                + "\"libName\":\"강남도서관\",\"address\":\"서울시\",\"latitude\":\"37.5\","
                                + "\"longitude\":\"127.0\",\"homepage\":\"http://lib\",\"closed\":\"월요일\","
                                + "\"operationTime\":\"09:00-18:00\"}}]}}",
                        MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.SUCCESS_HAS_DATA);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).libraryCode()).isEqualTo("111108");
    }

    @Test
    void search_response가_누락되면_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"error\":\"invalid key\"}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_libs가_비어있지_않은데_모든_항목이_malformed면_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess(
                        "{\"response\":{\"numFound\":2,\"libs\":["
                                + "{\"lib\":{\"address\":\"libCode/libName 없음\"}},"
                                + "{\"lib\":{\"libCode\":\"111108\"}}"
                                + "]}}",
                        MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_libs가_빈_배열이고_numFound가_있으면_정상_현재page빈결과로_처리한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":7,\"libs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 5, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.SUCCESS_HAS_DATA);
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(7);
    }

    @Test
    void search_libs가_누락되면_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("{\"response\":{\"numFound\":3}}", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_malformed_JSON은_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("이것은 JSON이 아닙니다", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_빈_body는_FAILED_RETRYABLE() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
    }

    @Test
    void search_HTTP_오류는_FAILED_RETRYABLE이며_authKey를_로그에_남기지_않는다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/libSrchByBook")))
                .andRespond(withServerError());

        InfoNaruLibraryClient client = new InfoNaruLibraryClient(builder.build(), enabledProperties());
        InfoNaruLibrarySearchResult result = client.search("9788966260959", "11", null, 0, 20);

        assertThat(result.status()).isEqualTo(InfoNaruLibraryFetchStatus.FAILED_RETRYABLE);
        assertThat(allLogMessages()).doesNotContain(AUTH_KEY);
    }
}
