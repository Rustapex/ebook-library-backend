package com.acorn.abc.admin.book.service;

import com.acorn.abc.admin.book.dto.EpubUploadResponse;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookEpub;
import com.acorn.abc.book.repository.BookEpubRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.service.BookEpubBinaryStore;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.repository.BookmarkRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminBookEpubServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookEpubRepository bookEpubRepository;
    @Mock private BookEpubBinaryStore bookEpubBinaryStore;
    @Mock private ReadingProgressRepository readingProgressRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private EpubFileValidator epubFileValidator;

    @InjectMocks private AdminBookEpubService adminBookEpubService;

    @Test
    void upload_성공하면_새버전응답과사용자읽기상태초기화를수행한다() {
        MultipartFile file = mock(MultipartFile.class);
        BookEpub saved = mock(BookEpub.class);
        given(bookRepository.findById(17L)).willReturn(Optional.of(mock(Book.class)));
        given(epubFileValidator.validate(file))
                .willReturn(new EpubValidationResult("book.epub", 128L, "a".repeat(64)));
        given(bookEpubBinaryStore.insertNextVersion(17L, "book.epub", 128L, "a".repeat(64), file))
                .willReturn(3);
        given(bookEpubRepository.findByBook_BookIdAndEpubVersion(17L, 3)).willReturn(Optional.of(saved));
        given(saved.getBookId()).willReturn(17L);
        given(saved.getOriginalFileName()).willReturn("book.epub");
        given(saved.getFileSize()).willReturn(128L);
        given(saved.getSha256()).willReturn("a".repeat(64));
        given(saved.getEpubVersion()).willReturn(3);
        given(readingProgressRepository.resetForEpubVersion(17L, 3)).willReturn(4);
        given(bookmarkRepository.deleteByRental_Book_BookId(17L)).willReturn(7L);

        EpubUploadResponse response = adminBookEpubService.upload(17L, file);

        assertThat(response.bookId()).isEqualTo(17L);
        assertThat(response.fileName()).isEqualTo("book.epub");
        assertThat(response.fileSize()).isEqualTo(128L);
        assertThat(response.sha256()).isEqualTo("a".repeat(64));
        assertThat(response.epubVersion()).isEqualTo(3);
        verify(readingProgressRepository).resetForEpubVersion(17L, 3);
        verify(bookmarkRepository).deleteByRental_Book_BookId(17L);
    }

    @Test
    void upload_도서가없으면_검증과저장및초기화를수행하지않는다() {
        MultipartFile file = mock(MultipartFile.class);
        given(bookRepository.findById(404L)).willReturn(Optional.empty());

        assertErrorCode(() -> adminBookEpubService.upload(404L, file), ErrorCode.BOOK_NOT_FOUND);

        verify(epubFileValidator, never()).validate(file);
        verify(bookEpubBinaryStore, never()).insertNextVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(MultipartFile.class));
        verify(readingProgressRepository, never()).resetForEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(bookmarkRepository, never()).deleteByRental_Book_BookId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void upload_파일검증실패이면_저장과초기화를수행하지않는다() {
        MultipartFile file = mock(MultipartFile.class);
        given(bookRepository.findById(17L)).willReturn(Optional.of(mock(Book.class)));
        given(epubFileValidator.validate(file))
                .willThrow(new BusinessException(ErrorCode.BOOK_EPUB_INVALID));

        assertErrorCode(() -> adminBookEpubService.upload(17L, file), ErrorCode.BOOK_EPUB_INVALID);

        verify(bookEpubBinaryStore, never()).insertNextVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(MultipartFile.class));
        verify(readingProgressRepository, never()).resetForEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(bookmarkRepository, never()).deleteByRental_Book_BookId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void upload_binary저장실패이면_사용자읽기상태를초기화하지않는다() {
        MultipartFile file = mock(MultipartFile.class);
        given(bookRepository.findById(17L)).willReturn(Optional.of(mock(Book.class)));
        given(epubFileValidator.validate(file))
                .willReturn(new EpubValidationResult("book.epub", 128L, "a".repeat(64)));
        given(bookEpubBinaryStore.insertNextVersion(17L, "book.epub", 128L, "a".repeat(64), file))
                .willThrow(new BusinessException(ErrorCode.BOOK_EPUB_INVALID));

        assertErrorCode(() -> adminBookEpubService.upload(17L, file), ErrorCode.BOOK_EPUB_INVALID);

        verify(readingProgressRepository, never()).resetForEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(bookmarkRepository, never()).deleteByRental_Book_BookId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void upload_저장된메타데이터가없으면_EPUB없음예외이고초기화하지않는다() {
        MultipartFile file = mock(MultipartFile.class);
        given(bookRepository.findById(17L)).willReturn(Optional.of(mock(Book.class)));
        given(epubFileValidator.validate(file))
                .willReturn(new EpubValidationResult("book.epub", 128L, "a".repeat(64)));
        given(bookEpubBinaryStore.insertNextVersion(17L, "book.epub", 128L, "a".repeat(64), file))
                .willReturn(3);
        given(bookEpubRepository.findByBook_BookIdAndEpubVersion(17L, 3)).willReturn(Optional.empty());

        assertErrorCode(() -> adminBookEpubService.upload(17L, file), ErrorCode.BOOK_EPUB_NOT_FOUND);

        verify(readingProgressRepository, never()).resetForEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(bookmarkRepository, never()).deleteByRental_Book_BookId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void upload_S3저장이실패하면_성공응답없이_예외를전파하고_초기화하지않는다() {
        MultipartFile file = mock(MultipartFile.class);
        given(bookRepository.findById(17L)).willReturn(Optional.of(mock(Book.class)));
        given(epubFileValidator.validate(file))
                .willReturn(new EpubValidationResult("book.epub", 128L, "a".repeat(64)));
        // S3 SDK 장애는 BusinessException이 아니라 전역 500 경로로 전파된다.
        given(bookEpubBinaryStore.insertNextVersion(17L, "book.epub", 128L, "a".repeat(64), file))
                .willThrow(new IllegalStateException("s3 unavailable"));

        assertThatThrownBy(() -> adminBookEpubService.upload(17L, file))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(BusinessException.class);

        verify(bookEpubRepository, never()).findByBook_BookIdAndEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(readingProgressRepository, never()).resetForEpubVersion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(bookmarkRepository, never()).deleteByRental_Book_BookId(org.mockito.ArgumentMatchers.anyLong());
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(expected);
    }
}
