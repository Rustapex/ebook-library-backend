package com.acorn.abc.admin.request.service;

import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateRequest;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateResponse;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.notification.service.NotificationService;
import com.acorn.abc.request.domain.BookRequest;
import com.acorn.abc.request.domain.BookRequestCandidate;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.request.repository.BookRequestRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminBookRequestServiceTest {

    @Mock
    private BookRequestCandidateRepository candidateRepository;
    @Mock
    private BookRequestRepository bookRequestRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    @InjectMocks
    private AdminBookRequestService adminBookRequestService;

    @Test
    void updateStatus_IN_REVIEW_성공하면_bookRequest는_건드리지_않는다() {
        given(candidateRepository.moveToInReviewIfRequested(1L)).willReturn(1);
        given(bookRequestRepository.countByRequestCandidate_RequestCandidateIdAndRequestStatus(1L, "REQUESTED"))
                .willReturn(3L);

        AdminBookRequestStatusUpdateResponse response = adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("IN_REVIEW", null, null));

        assertThat(response.status()).isEqualTo("IN_REVIEW");
        assertThat(response.affectedRequestCount()).isEqualTo(3);
        verify(bookRequestRepository, never()).updateStatusForRequestedByCandidateId(any(), any(), any(), any(), any());
        verify(adminAuditLogWriter).record(eq(9L), eq(AdminAuditLogActionType.BOOK_REQUEST_STATUS_UPDATE),
                eq(AdminAuditLogTargetType.BOOK_REQUEST_CANDIDATE), eq(1L), isNull(), any());
    }

    @Test
    void updateStatus_IN_REVIEW_대상후보가_없으면_404() {
        given(candidateRepository.moveToInReviewIfRequested(1L)).willReturn(0);
        given(candidateRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("IN_REVIEW", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_REQUEST_CANDIDATE_NOT_FOUND);
    }

    @Test
    void updateStatus_IN_REVIEW_이미_종료된_후보면_409() {
        given(candidateRepository.moveToInReviewIfRequested(1L)).willReturn(0);
        given(candidateRepository.existsById(1L)).willReturn(true);

        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("IN_REVIEW", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_REQUEST_INVALID_STATUS);
    }

    @Test
    void updateStatus_APPROVED에_승인도서가_없으면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("APPROVED", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(candidateRepository, bookRequestRepository, bookRepository);
    }

    @Test
    void updateStatus_REJECTED에_사유가_없으면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("REJECTED", null, "   ")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateStatus_REJECTED에_승인도서ID를_같이보내면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("REJECTED", 5L, "사유")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateStatus_IN_REVIEW에_필드가_있으면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("IN_REVIEW", 5L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateStatus_APPROVED_정상처리시_알림과_감사로그를_남긴다() {
        Book book = mock(Book.class);
        given(bookRepository.findById(5L)).willReturn(Optional.of(book));

        Member member = mock(Member.class);
        BookRequestCandidate candidate = mock(BookRequestCandidate.class);
        BookRequest br = BookRequest.create(candidate, member, "제목", "저자", "출판사",
                "제목", "저자", "출판사", "사유", LocalDateTime.now());
        given(bookRequestRepository.findRequestedByCandidateId(1L)).willReturn(List.of(br));

        given(candidateRepository.finalizeIfEligible(1L, "APPROVED")).willReturn(1);
        given(bookRequestRepository.updateStatusForRequestedByCandidateId(eq(1L), eq("APPROVED"), eq(book), isNull(), any()))
                .willReturn(1);

        AdminBookRequestStatusUpdateResponse response = adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("APPROVED", 5L, null));

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.affectedRequestCount()).isEqualTo(1);
        verify(notificationService).notifyMembers(eq(List.of(member)), eq("BOOK_REQUEST"), any(), eq("BOOK_REQUEST_CANDIDATE"), eq(1L));
        verify(adminAuditLogWriter).record(eq(9L), eq(AdminAuditLogActionType.BOOK_REQUEST_STATUS_UPDATE),
                eq(AdminAuditLogTargetType.BOOK_REQUEST_CANDIDATE), eq(1L), isNull(), any());
    }

    @Test
    void updateStatus_벌크UPDATE가_0건이면_후보변경도_롤백되도록_예외를_던진다() {
        Book book = mock(Book.class);
        given(bookRepository.findById(5L)).willReturn(Optional.of(book));
        given(bookRequestRepository.findRequestedByCandidateId(1L)).willReturn(List.of());
        given(candidateRepository.finalizeIfEligible(1L, "APPROVED")).willReturn(1);
        given(bookRequestRepository.updateStatusForRequestedByCandidateId(eq(1L), eq("APPROVED"), eq(book), isNull(), any()))
                .willReturn(0);

        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("APPROVED", 5L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_REQUEST_INVALID_STATUS);

        verify(notificationService, never()).notifyMembers(any(), any(), any(), any(), any());
    }

    @Test
    void updateStatus_APPROVED_존재하지_않는_도서면_BOOK_NOT_FOUND() {
        given(bookRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("APPROVED", 5L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);

        verify(candidateRepository, never()).finalizeIfEligible(any(), any());
    }

    @Test
    void updateStatus_IN_REVIEW에_공백_반려사유_키가_있으면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("IN_REVIEW", null, "   ")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateStatus_APPROVED에_공백_반려사유_키가_있으면_VALIDATION_ERROR() {
        assertThatThrownBy(() -> adminBookRequestService.updateStatus(
                9L, 1L, new AdminBookRequestStatusUpdateRequest("APPROVED", 5L, "   ")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
