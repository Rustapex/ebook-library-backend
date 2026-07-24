package com.acorn.abc.rental.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookEpub;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.book.service.BookEpubBinaryStore;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.EpubDownloadMetadata;
import com.acorn.abc.rental.dto.ProgressUpdateRequest;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewerServiceTest {

    @Mock private RentalService rentalService;
    @Mock private BookEpubRepository bookEpubRepository;
    @Mock private BookEpubBinaryStore bookEpubBinaryStore;
    @Mock private ReadingProgressRepository readingProgressRepository;

    @InjectMocks private ViewerService viewerService;

    @Test
    void saveProgress_staleEpubVersion_409Conflict를_반환한다() {
        BookRental rental = mock(BookRental.class);
        Book book = mock(Book.class);
        BookEpub latest = mock(BookEpub.class);
        ReadingProgress progress = mock(ReadingProgress.class);
        given(rentalService.getOwnedRentalForUpdateOrThrow(5L, 10L)).willReturn(rental);
        given(rental.getBook()).willReturn(book);
        given(book.getBookId()).willReturn(1L);
        given(bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(1L))
                .willReturn(Optional.of(latest));
        given(latest.getEpubVersion()).willReturn(2);
        given(readingProgressRepository.findByRentalIdForUpdate(5L)).willReturn(Optional.of(progress));
        given(progress.getRevision()).willReturn(7L);

        ProgressUpdateRequest request = new ProgressUpdateRequest(
                1, "epubcfi(/6/2)", "chapter.xhtml", 0, 0.5, false, 7L);

        assertThatThrownBy(() -> viewerService.saveProgress(5L, request, 10L))
                .isInstanceOf(ProgressRevisionConflictException.class)
                .extracting("latestRevision")
                .isEqualTo(7L);
        verify(readingProgressRepository, never()).saveAndFlush(progress);
    }

    @Test
    void prepareDownload_최초Progress생성전에_rentalLock을_사용한다() {
        BookRental rental = mock(BookRental.class);
        Book book = mock(Book.class);
        BookEpub latest = mock(BookEpub.class);
        given(rentalService.getOwnedRentalForUpdateOrThrow(5L, 10L)).willReturn(rental);
        given(rental.getBook()).willReturn(book);
        given(book.getBookId()).willReturn(1L);
        given(bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(1L))
                .willReturn(Optional.of(latest));
        given(latest.getBookId()).willReturn(1L);
        given(latest.getEpubVersion()).willReturn(2);
        given(latest.getOriginalFileName()).willReturn("book.epub");
        given(latest.getSha256()).willReturn("a".repeat(64));
        given(readingProgressRepository.findByRental_RentalId(5L)).willReturn(Optional.empty());

        viewerService.prepareDownload(5L, 10L);

        verify(rentalService).getOwnedRentalForUpdateOrThrow(5L, 10L);
        verify(readingProgressRepository).save(org.mockito.ArgumentMatchers.any(ReadingProgress.class));
    }

    @Test
    void prepareDownload_응답계약은_bookId_version_파일명_크기_sha256을_그대로유지한다() {
        BookRental rental = mock(BookRental.class);
        Book book = mock(Book.class);
        BookEpub latest = mock(BookEpub.class);
        given(rentalService.getOwnedRentalForUpdateOrThrow(5L, 10L)).willReturn(rental);
        given(rental.getBook()).willReturn(book);
        given(book.getBookId()).willReturn(1L);
        given(bookEpubRepository.findFirstByBook_BookIdOrderByEpubVersionDesc(1L))
                .willReturn(Optional.of(latest));
        given(latest.getBookId()).willReturn(1L);
        given(latest.getEpubVersion()).willReturn(2);
        given(latest.getOriginalFileName()).willReturn("book.epub");
        given(latest.getFileSize()).willReturn(2048L);
        given(latest.getSha256()).willReturn("a".repeat(64));
        given(readingProgressRepository.findByRental_RentalId(5L)).willReturn(Optional.empty());

        EpubDownloadMetadata metadata = viewerService.prepareDownload(5L, 10L);

        assertThat(metadata.bookId()).isEqualTo(1L);
        assertThat(metadata.epubVersion()).isEqualTo(2);
        assertThat(metadata.fileName()).isEqualTo("book.epub");
        assertThat(metadata.fileSize()).isEqualTo(2048L);
        assertThat(metadata.sha256()).isEqualTo("a".repeat(64));
    }

    @Test
    void writeEpub은_저장소종류와무관하게_BinaryStore로_위임한다() {
        EpubDownloadMetadata metadata = new EpubDownloadMetadata(1L, 2, "book.epub", 2048L, "a".repeat(64));
        OutputStream output = new ByteArrayOutputStream();

        viewerService.writeEpub(metadata, output);

        verify(bookEpubBinaryStore).writeTo(1L, 2, output);
    }
}
