package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnEnrichResponse;
import com.acorn.abc.admin.book.dto.BookIsbnTempFetchResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BookIsbnEnrichServiceTest {

    @Mock
    private BookIsbnTempRepository bookIsbnTempRepository;
    @Mock
    private BookIsbnTempService bookIsbnTempService;

    private BookIsbnEnrichService service() {
        return new BookIsbnEnrichService(bookIsbnTempRepository, bookIsbnTempService);
    }

    @Test
    void enrich_성공과_실패를_분리해서_집계한다() throws Exception {
        BookIsbnTemp temp1 = BookIsbnTemp.create("9788966260001");
        BookIsbnTemp temp2 = BookIsbnTemp.create("9788966260002");
        given(bookIsbnTempRepository.findByStatusCd(eq(BookIsbnTemp.STATUS_PENDING), any()))
                .willReturn(new PageImpl<>(List.of(temp1, temp2)));

        given(bookIsbnTempService.fetch("9788966260001")).willReturn(
                new BookIsbnTempFetchResponse(1L, "9788966260001", BookIsbnTemp.STATUS_READY, null, null, null));
        given(bookIsbnTempService.fetch("9788966260002"))
                .willThrow(new RuntimeException("provider timeout"));

        BookIsbnEnrichResponse response = service().enrich(10);

        assertThat(response.attemptedCount()).isEqualTo(2);
        assertThat(response.readyCount()).isEqualTo(1);
        assertThat(response.failedIsbns()).containsExactly("9788966260002");
    }

    @Test
    void enrich_여전히_PENDING이면_실패목록에_포함한다() throws Exception {
        BookIsbnTemp temp = BookIsbnTemp.create("9788966260003");
        given(bookIsbnTempRepository.findByStatusCd(eq(BookIsbnTemp.STATUS_PENDING), any()))
                .willReturn(new PageImpl<>(List.of(temp)));
        given(bookIsbnTempService.fetch("9788966260003")).willReturn(
                new BookIsbnTempFetchResponse(1L, "9788966260003", BookIsbnTemp.STATUS_PENDING, null, null, null));

        BookIsbnEnrichResponse response = service().enrich(10);

        assertThat(response.readyCount()).isZero();
        assertThat(response.failedIsbns()).containsExactly("9788966260003");
    }
}
