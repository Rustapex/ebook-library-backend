package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnSeedRequest;
import com.acorn.abc.admin.book.dto.BookIsbnSeedResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.external.book.client.InfoNaruBookListClient;
import com.acorn.abc.external.book.dto.ExternalBookSeedCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookIsbnSeedServiceTest {

    @Mock
    private InfoNaruBookListClient infoNaruBookListClient;
    @Mock
    private BookIsbnTempRepository bookIsbnTempRepository;
    @Mock
    private BookRepository bookRepository;

    private BookIsbnSeedService service() {
        return new BookIsbnSeedService(infoNaruBookListClient, bookIsbnTempRepository, bookRepository);
    }

    @Test
    void seed_targetCount에_도달하면_중간에_종료한다() {
        given(bookIsbnTempRepository.findByIsbn13(any())).willReturn(Optional.empty());
        given(bookRepository.existsByIsbn(any())).willReturn(false);
        given(infoNaruBookListClient.search("컴퓨터", 1, 2)).willReturn(List.of(
                new ExternalBookSeedCandidate("9788966260001", "책1", "출판사"),
                new ExternalBookSeedCandidate("9788966260002", "책2", "출판사")));

        BookIsbnSeedResponse response = service().seed(new BookIsbnSeedRequest("컴퓨터", 1, 2, 3));

        assertThat(response.collectedCount()).isEqualTo(1);
        assertThat(response.pagesFetched()).isEqualTo(1);
        assertThat(response.collectedIsbns()).containsExactly("9788966260001");
        verify(infoNaruBookListClient, times(1)).search(any(), anyInt(), anyInt());
        verify(bookIsbnTempRepository, times(1)).save(any());
    }

    @Test
    void seed_이미_temp에_있는_ISBN은_건너뛴다() {
        given(bookIsbnTempRepository.findByIsbn13("9788966260001"))
                .willReturn(Optional.of(mock(BookIsbnTemp.class)));
        given(infoNaruBookListClient.search(any(), eq(1), anyInt())).willReturn(
                List.of(new ExternalBookSeedCandidate("9788966260001", "책1", "출판사")));
        given(infoNaruBookListClient.search(any(), eq(2), anyInt())).willReturn(List.of());

        BookIsbnSeedResponse response = service().seed(new BookIsbnSeedRequest("컴퓨터", 5, 10, 2));

        assertThat(response.collectedCount()).isZero();
        verify(bookIsbnTempRepository, never()).save(any());
    }

    @Test
    void seed_결과가_없으면_페이지_수집을_중단한다() {
        given(infoNaruBookListClient.search(any(), eq(1), anyInt())).willReturn(List.of());

        BookIsbnSeedResponse response = service().seed(new BookIsbnSeedRequest("컴퓨터", 5, 10, 3));

        assertThat(response.pagesFetched()).isEqualTo(1);
        verify(infoNaruBookListClient, times(1)).search(any(), anyInt(), anyInt());
    }
}
