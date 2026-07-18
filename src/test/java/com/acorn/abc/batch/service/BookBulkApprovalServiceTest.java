package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnBulkApproveRequest;
import com.acorn.abc.admin.book.dto.BookIsbnBulkApproveResponse;
import com.acorn.abc.admin.book.dto.BookIsbnTempApproveRequest;
import com.acorn.abc.admin.book.dto.BookIsbnTempApproveResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookBulkApprovalServiceTest {

    @Mock
    private BookIsbnTempRepository bookIsbnTempRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookApprovalService bookApprovalService;

    private BookBulkApprovalService service() {
        return new BookBulkApprovalService(bookIsbnTempRepository, bookRepository, bookApprovalService);
    }

    private BookIsbnTemp readyTemp(long id, String isbn, String title, String publisher, String author,
                                    String aladinCategoryId, String aladinCategoryName) {
        BookIsbnTemp temp = BookIsbnTemp.create(isbn);
        temp.applyMetadata(title, publisher, author, null, "설명", null,
                aladinCategoryId, aladinCategoryName, (byte) 1, (byte) 1);
        temp.markReady();
        ReflectionTestUtils.setField(temp, "id", id);
        return temp;
    }

    @Test
    void bulkApprove_자동승인_조건을_만족하지_않으면_스킵한다() {
        BookIsbnTemp missingAuthor = readyTemp(1L, "9788966260001", "제목", "출판사", null, "2724", "카테고리");
        given(bookIsbnTempRepository.findAllById(List.of(1L))).willReturn(List.of(missingAuthor));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(List.of(1L), 10, 100, "PAID", 5000, "AVAILABLE");

        BookIsbnBulkApproveResponse response = service().bulkApprove(request, 99L);

        assertThat(response.approvedCount()).isZero();
        assertThat(response.failures()).hasSize(1);
        assertThat(response.failures().get(0).reason()).isEqualTo("저자 없음");
        verify(bookApprovalService, never()).approve(any(), any(), any());
    }

    @Test
    void bulkApprove_limit에_도달하면_중단한다() {
        BookIsbnTemp temp1 = readyTemp(1L, "9788966260001", "제목1", "출판사", "저자 (지은이)", "2724", "카테고리");
        BookIsbnTemp temp2 = readyTemp(2L, "9788966260002", "제목2", "출판사", "저자 (지은이)", "2724", "카테고리");
        given(bookIsbnTempRepository.findByStatusCd(eq(BookIsbnTemp.STATUS_READY), any()))
                .willReturn(new PageImpl<>(List.of(temp1, temp2)));
        given(bookApprovalService.approve(any(), any(), any()))
                .willReturn(new BookIsbnTempApproveResponse(100L, 1L, "AVAILABLE"));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(null, 1, 100, "PAID", 5000, "AVAILABLE");

        BookIsbnBulkApproveResponse response = service().bulkApprove(request, 99L);

        assertThat(response.approvedCount()).isEqualTo(1);
        verify(bookApprovalService, times(1)).approve(any(), any(), any());
    }

    @Test
    void bulkApprove_maxCandidates에_도달하면_승인수와_무관하게_스캔을_중단한다() {
        BookIsbnTemp missingAuthor1 = readyTemp(1L, "9788966260010", "제목1", "출판사", null, "2724", "카테고리");
        BookIsbnTemp missingAuthor2 = readyTemp(2L, "9788966260011", "제목2", "출판사", null, "2724", "카테고리");
        BookIsbnTemp eligible = readyTemp(3L, "9788966260012", "제목3", "출판사", "저자 (지은이)", "2724", "카테고리");
        given(bookIsbnTempRepository.findByStatusCd(eq(BookIsbnTemp.STATUS_READY), any()))
                .willReturn(new PageImpl<>(List.of(missingAuthor1, missingAuthor2, eligible)));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(null, 30, 2, "PAID", 5000, "AVAILABLE");

        BookIsbnBulkApproveResponse response = service().bulkApprove(request, 99L);

        assertThat(response.approvedCount()).isZero();
        assertThat(response.failures()).hasSize(2);
        verify(bookApprovalService, never()).approve(any(), any(), any());
    }

    @Test
    void bulkApprove_author와_categoryIds를_생성해서_approve에_전달한다() {
        BookIsbnTemp temp = readyTemp(1L, "9788966260003", "클린코드", "인사이트",
                "마틴 (지은이), 김번역 (옮긴이)", "2724", "카테고리");
        given(bookIsbnTempRepository.findAllById(List.of(1L))).willReturn(List.of(temp));
        given(bookApprovalService.approve(any(), any(), any()))
                .willReturn(new BookIsbnTempApproveResponse(200L, 1L, "AVAILABLE"));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(List.of(1L), 10, 100, "PAID", 5000, "AVAILABLE");

        service().bulkApprove(request, 99L);

        ArgumentCaptor<BookIsbnTempApproveRequest> captor = ArgumentCaptor.forClass(BookIsbnTempApproveRequest.class);
        verify(bookApprovalService).approve(eq(1L), captor.capture(), eq(99L));
        BookIsbnTempApproveRequest captured = captor.getValue();

        assertThat(captured.categoryIds()).isEmpty();
        assertThat(captured.authors()).containsExactly("마틴", "김번역");
    }

    @Test
    void bulkApprove_BusinessException은_한건만_실패처리하고_계속_승인한다() {
        BookIsbnTemp temp1 = readyTemp(1L, "9788966260004", "제목1", "출판사", "저자 (지은이)", "2724", "카테고리");
        BookIsbnTemp temp2 = readyTemp(2L, "9788966260005", "제목2", "출판사", "저자 (지은이)", "2724", "카테고리");
        given(bookIsbnTempRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(temp1, temp2));
        given(bookApprovalService.approve(eq(1L), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        given(bookApprovalService.approve(eq(2L), any(), any()))
                .willReturn(new BookIsbnTempApproveResponse(300L, 2L, "AVAILABLE"));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(List.of(1L, 2L), 10, 100, "PAID", 5000, "AVAILABLE");

        BookIsbnBulkApproveResponse response = service().bulkApprove(request, 99L);

        assertThat(response.approvedCount()).isEqualTo(1);
        assertThat(response.approvedBookIds()).containsExactly(300L);
        assertThat(response.failures()).hasSize(1);
        assertThat(response.failures().get(0).reason()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    void bulkApprove_예상하지_못한_예외는_잡지_않고_그대로_전파한다() {
        BookIsbnTemp temp = readyTemp(1L, "9788966260006", "제목", "출판사", "저자 (지은이)", "2724", "카테고리");
        given(bookIsbnTempRepository.findAllById(List.of(1L))).willReturn(List.of(temp));
        given(bookApprovalService.approve(eq(1L), any(), any()))
                .willThrow(new RuntimeException("DB connection lost"));

        BookIsbnBulkApproveRequest request =
                new BookIsbnBulkApproveRequest(List.of(1L), 10, 100, "PAID", 5000, "AVAILABLE");

        assertThatThrownBy(() -> service().bulkApprove(request, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection lost");
    }
}
