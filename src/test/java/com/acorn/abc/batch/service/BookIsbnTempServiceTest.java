package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.ExternalBookDuplicateInfo;
import com.acorn.abc.admin.book.dto.ExternalBookLookupResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.external.book.dto.BookMetadataResult;
import com.acorn.abc.external.book.dto.DuplicateStatus;
import com.acorn.abc.external.book.dto.ProviderStatus;
import com.acorn.abc.external.book.service.BookMetadataAggregateService;
import com.acorn.abc.external.book.service.DuplicateBookDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookIsbnTempServiceTest {

    private static final String RAW_ISBN = "978-89-6626-095-9";
    private static final String ISBN13 = "9788966260959";

    @Mock
    private BookMetadataAggregateService aggregateService;
    @Mock
    private DuplicateBookDetector duplicateBookDetector;
    @Mock
    private BookIsbnTempRepository bookIsbnTempRepository;

    private BookIsbnTempService service() {
        return new BookIsbnTempService(aggregateService, duplicateBookDetector, bookIsbnTempRepository);
    }

    @Test
    void lookup_mapsAggregatedMetadataAndDuplicateWarningWithoutSaving() {
        BookMetadataResult metadata = metadata("Effective Java");
        ExternalBookDuplicateInfo duplicate = new ExternalBookDuplicateInfo(
                DuplicateStatus.BOOK_EXISTS.name(),
                10L,
                List.of(new ExternalBookDuplicateInfo.Candidate(
                        10L, "Effective Java", ISBN13, "AVAILABLE")));
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(duplicate);

        ExternalBookLookupResponse response = service().lookup(RAW_ISBN);

        assertThat(response.isbn13()).isEqualTo(ISBN13);
        assertThat(response.title()).isEqualTo(metadata.title());
        assertThat(response.author()).isEqualTo(metadata.author());
        assertThat(response.publisher()).isEqualTo(metadata.publisher());
        assertThat(response.providers().infonaru()).isEqualTo(ProviderStatus.SUCCESS_HAS_DATA.name());
        assertThat(response.providers().naver()).isEqualTo(ProviderStatus.FAILED_RETRYABLE.name());
        assertThat(response.providers().aladin()).isEqualTo(ProviderStatus.SUCCESS_NO_DATA.name());
        assertThat(response.duplicate()).isSameAs(duplicate);
        assertThat(response.warnings()).contains("provider warning").hasSize(2);
        verifyNoInteractions(bookIsbnTempRepository);
    }

    @Test
    void fetch_newMetadataWithTitle_savesReadyTempAndReturnsSavedResult() {
        BookMetadataResult metadata = metadata("Effective Java");
        ExternalBookDuplicateInfo duplicate = noDuplicate();
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(bookIsbnTempRepository.findByIsbn13(ISBN13)).willReturn(Optional.empty());
        given(bookIsbnTempRepository.save(any(BookIsbnTemp.class))).willAnswer(invocation -> {
            BookIsbnTemp saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 35L);
            return saved;
        });
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(duplicate);

        var response = service().fetch(RAW_ISBN);

        ArgumentCaptor<BookIsbnTemp> captor = ArgumentCaptor.forClass(BookIsbnTemp.class);
        verify(bookIsbnTempRepository).save(captor.capture());
        BookIsbnTemp saved = captor.getValue();
        assertThat(saved.getIsbn13()).isEqualTo(ISBN13);
        assertThat(saved.getBookTitle()).isEqualTo(metadata.title());
        assertThat(saved.getPublisher()).isEqualTo(metadata.publisher());
        assertThat(saved.getAuthor()).isEqualTo(metadata.author());
        assertThat(saved.getNaverResStatus()).isEqualTo(ProviderStatus.FAILED_RETRYABLE.code());
        assertThat(saved.getAladinResStatus()).isEqualTo(ProviderStatus.SUCCESS_NO_DATA.code());
        assertThat(saved.getStatusCd()).isEqualTo(BookIsbnTemp.STATUS_READY);
        assertThat(response.tempId()).isEqualTo(35L);
        assertThat(response.isbn13()).isEqualTo(ISBN13);
        assertThat(response.statusCd()).isEqualTo(BookIsbnTemp.STATUS_READY);
        assertThat(response.duplicate()).isSameAs(duplicate);
    }

    @Test
    void fetch_newMetadataWithoutTitle_savesPendingTemp() {
        BookMetadataResult metadata = metadata(null);
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(bookIsbnTempRepository.findByIsbn13(ISBN13)).willReturn(Optional.empty());
        given(bookIsbnTempRepository.save(any(BookIsbnTemp.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(noDuplicate());

        var response = service().fetch(RAW_ISBN);

        ArgumentCaptor<BookIsbnTemp> captor = ArgumentCaptor.forClass(BookIsbnTemp.class);
        verify(bookIsbnTempRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusCd()).isEqualTo(BookIsbnTemp.STATUS_PENDING);
        assertThat(response.statusCd()).isEqualTo(BookIsbnTemp.STATUS_PENDING);
    }

    @Test
    void fetch_existingSeed_preservesTitleWhenProviderTitleIsBlankAndMarksReady() {
        BookIsbnTemp existing = BookIsbnTemp.createSeed(ISBN13, "Seed title", "Seed publisher");
        ReflectionTestUtils.setField(existing, "id", 7L);
        BookMetadataResult metadata = metadata(" ");
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(bookIsbnTempRepository.findByIsbn13(ISBN13)).willReturn(Optional.of(existing));
        given(bookIsbnTempRepository.save(existing)).willReturn(existing);
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(noDuplicate());

        var response = service().fetch(RAW_ISBN);

        assertThat(existing.getBookTitle()).isEqualTo("Seed title");
        assertThat(existing.getPublisher()).isEqualTo(metadata.publisher());
        assertThat(existing.getStatusCd()).isEqualTo(BookIsbnTemp.STATUS_READY);
        assertThat(response.tempId()).isEqualTo(7L);
        assertThat(response.statusCd()).isEqualTo(BookIsbnTemp.STATUS_READY);
    }

    @Test
    void fetch_existingMergedTemp_updatesMetadataAndReturnsSavedResultWithoutChangingStatus() {
        BookIsbnTemp existing = BookIsbnTemp.createSeed(ISBN13, "Old title", "Old publisher");
        ReflectionTestUtils.setField(existing, "id", 99L);
        existing.markMerged();
        BookMetadataResult metadata = metadata("Updated title");
        ExternalBookDuplicateInfo duplicate = noDuplicate();
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(bookIsbnTempRepository.findByIsbn13(ISBN13)).willReturn(Optional.of(existing));
        given(bookIsbnTempRepository.save(existing)).willReturn(existing);
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(duplicate);

        var response = service().fetch(RAW_ISBN);

        assertThat(existing.getBookTitle()).isEqualTo("Updated title");
        assertThat(existing.getPublisher()).isEqualTo(metadata.publisher());
        assertThat(existing.getStatusCd()).isEqualTo(BookIsbnTemp.STATUS_MERGED);
        verify(bookIsbnTempRepository).save(existing);
        assertThat(response.tempId()).isEqualTo(99L);
        assertThat(response.statusCd()).isEqualTo(BookIsbnTemp.STATUS_MERGED);
        assertThat(response.aladinCategoryId()).isEqualTo(metadata.aladinCategoryId());
        assertThat(response.aladinCategoryName()).isEqualTo(metadata.aladinCategoryName());
        assertThat(response.duplicate()).isSameAs(duplicate);
    }

    @Test
    void fetch_existingMergedTempWithoutTitle_keepsMergedStatusInEntityAndResponse() {
        BookIsbnTemp existing = BookIsbnTemp.create(ISBN13);
        ReflectionTestUtils.setField(existing, "id", 100L);
        existing.markMerged();
        BookMetadataResult metadata = metadata(null);
        given(aggregateService.aggregate(RAW_ISBN)).willReturn(metadata);
        given(bookIsbnTempRepository.findByIsbn13(ISBN13)).willReturn(Optional.of(existing));
        given(bookIsbnTempRepository.save(existing)).willReturn(existing);
        given(duplicateBookDetector.detect(
                metadata.isbn13(), metadata.title(), metadata.author(), metadata.publisher()))
                .willReturn(noDuplicate());

        var response = service().fetch(RAW_ISBN);

        assertThat(existing.getBookTitle()).isNull();
        assertThat(existing.getStatusCd()).isEqualTo(BookIsbnTemp.STATUS_MERGED);
        verify(bookIsbnTempRepository).save(existing);
        assertThat(response.tempId()).isEqualTo(100L);
        assertThat(response.statusCd()).isEqualTo(BookIsbnTemp.STATUS_MERGED);
    }

    @Test
    void list_delegatesStatusAndPageableToRepository() {
        PageRequest pageable = PageRequest.of(1, 20);
        Page<BookIsbnTemp> expected = new PageImpl<>(List.of(BookIsbnTemp.create(ISBN13)), pageable, 21);
        given(bookIsbnTempRepository.findByStatusCd(BookIsbnTemp.STATUS_READY, pageable))
                .willReturn(expected);

        Page<BookIsbnTemp> result = service().list(BookIsbnTemp.STATUS_READY, pageable);

        assertThat(result).isSameAs(expected);
        verify(bookIsbnTempRepository).findByStatusCd(BookIsbnTemp.STATUS_READY, pageable);
    }

    @Test
    void fetch_aggregateFailure_propagatesAndDoesNotPersistOrDetectDuplicates() {
        IllegalStateException failure = new IllegalStateException("provider failure");
        given(aggregateService.aggregate(RAW_ISBN)).willThrow(failure);

        assertThatThrownBy(() -> service().fetch(RAW_ISBN))
                .isSameAs(failure);
        verifyNoInteractions(bookIsbnTempRepository, duplicateBookDetector);
    }

    private BookMetadataResult metadata(String title) {
        return new BookMetadataResult(
                ISBN13,
                title,
                "Joshua Bloch",
                "Addison-Wesley",
                "https://example.com/cover.jpg",
                "description",
                "2018-01-06",
                "123",
                "Programming > Java",
                ProviderStatus.SUCCESS_HAS_DATA,
                ProviderStatus.FAILED_RETRYABLE,
                ProviderStatus.SUCCESS_NO_DATA,
                List.of("provider warning"));
    }

    private ExternalBookDuplicateInfo noDuplicate() {
        return new ExternalBookDuplicateInfo(DuplicateStatus.NONE.name(), null, List.of());
    }
}
