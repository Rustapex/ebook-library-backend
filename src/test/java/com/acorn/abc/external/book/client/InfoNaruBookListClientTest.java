package com.acorn.abc.external.book.client;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.external.book.config.BookApiProperties;
import com.acorn.abc.external.book.dto.ExternalBookSeedCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InfoNaruBookListClientTest {

    private BookApiProperties enabledProperties() {
        BookApiProperties properties = new BookApiProperties();
        properties.setEnabled(true);
        properties.getInfonaru().setKey("infokey");
        return properties;
    }

    @Test
    void search_유효한_ISBN13만_후보로_반환한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/srchBooks")))
                .andRespond(withSuccess(
                        "{\"response\":{\"docs\":["
                                + "{\"doc\":{\"isbn13\":\"9788966260959\",\"bookname\":\"클린코드\",\"publisher\":\"인사이트\"}},"
                                + "{\"doc\":{\"isbn13\":\"invalid\",\"bookname\":\"X\",\"publisher\":\"Y\"}},"
                                + "{\"doc\":{\"isbn13\":\"9788966260960\",\"bookname\":null,\"publisher\":\"인사이트\"}}"
                                + "]}}",
                        MediaType.APPLICATION_JSON));

        InfoNaruBookListClient client = new InfoNaruBookListClient(builder.build(), enabledProperties());
        List<ExternalBookSeedCandidate> candidates = client.search("컴퓨터", 1, 50);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).isbn13()).isEqualTo("9788966260959");
        assertThat(candidates.get(0).title()).isEqualTo("클린코드");
        assertThat(candidates.get(0).publisher()).isEqualTo("인사이트");
        server.verify();
    }

    @Test
    void search_docs가_없으면_빈리스트를_반환한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/srchBooks")))
                .andRespond(withSuccess("{\"response\":{\"docs\":[]}}", MediaType.APPLICATION_JSON));

        InfoNaruBookListClient client = new InfoNaruBookListClient(builder.build(), enabledProperties());

        assertThat(client.search("컴퓨터", 1, 50)).isEmpty();
    }

    @Test
    void search_비활성화_상태면_설정오류로_즉시_실패한다() {
        BookApiProperties properties = enabledProperties();
        properties.setEnabled(false);
        InfoNaruBookListClient client = new InfoNaruBookListClient(
                RestClient.builder().baseUrl("http://data4library.kr").build(), properties);

        assertThatThrownBy(() -> client.search("컴퓨터", 1, 50))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_BOOK_API_DISABLED);
    }

    @Test
    void search_인증키가_없으면_설정오류로_즉시_실패한다() {
        BookApiProperties properties = enabledProperties();
        properties.getInfonaru().setKey("");
        InfoNaruBookListClient client = new InfoNaruBookListClient(
                RestClient.builder().baseUrl("http://data4library.kr").build(), properties);

        assertThatThrownBy(() -> client.search("컴퓨터", 1, 50))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_BOOK_API_DISABLED);
    }

    @Test
    void search_HTTP_오류는_provider_호출실패로_처리한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/srchBooks")))
                .andRespond(withServerError());

        InfoNaruBookListClient client = new InfoNaruBookListClient(builder.build(), enabledProperties());

        assertThatThrownBy(() -> client.search("컴퓨터", 1, 50))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_BOOK_API_CALL_FAILED);
    }

    @Test
    void search_응답_파싱_실패는_provider_호출실패로_처리한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data4library.kr");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/srchBooks")))
                .andRespond(withSuccess("이것은 JSON이 아닙니다", MediaType.APPLICATION_JSON));

        InfoNaruBookListClient client = new InfoNaruBookListClient(builder.build(), enabledProperties());

        assertThatThrownBy(() -> client.search("컴퓨터", 1, 50))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_BOOK_API_CALL_FAILED);
    }
}
