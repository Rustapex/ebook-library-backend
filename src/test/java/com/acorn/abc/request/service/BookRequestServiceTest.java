package com.acorn.abc.request.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.request.domain.BookRequest;
import com.acorn.abc.request.domain.BookRequestCandidate;
import com.acorn.abc.request.dto.BookRequestCreateRequest;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.dto.BookRequestHistoryItemResponse;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.request.repository.BookRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookRequestServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BookRequestRepository bookRequestRepository;
    @Mock
    private BookRequestCandidateRepository bookRequestCandidateRepository;

    @InjectMocks
    private BookRequestService bookRequestService;

    @Test
    void createBookRequest_정상요청이면_생성된다() {
        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(bookRequestRepository.existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
                eq(1L), anyString(), anyString(), anyString())).willReturn(false);
        given(bookRequestCandidateRepository.lastInsertId()).willReturn(100L);

        BookRequestCandidate candidate = BookRequestCandidate.createNew("제목", "저자", "출판사", LocalDateTime.now());
        given(bookRequestCandidateRepository.getReferenceById(100L)).willReturn(candidate);
        given(bookRequestRepository.saveAndFlush(any(BookRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(bookRequestCandidateRepository.findById(100L)).willReturn(Optional.of(candidate));

        BookRequestCreateResponse response = bookRequestService.createBookRequest(
                1L, new BookRequestCreateRequest("제목", "저자", "출판사", "이유"));

        assertThat(response.requestCandidateId()).isEqualTo(100L);
        assertThat(response.requestStatus()).isEqualTo("REQUESTED");
        assertThat(response.requestCount()).isEqualTo(1);
        verify(bookRequestCandidateRepository).upsertCandidate(eq("제목"), eq("저자"), eq("출판사"), any());
    }

    @Test
    void createBookRequest_이미_신청한_조합이면_409를_던지고_후보를_건드리지_않는다() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(bookRequestRepository.existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
                eq(1L), anyString(), anyString(), anyString())).willReturn(true);

        assertThatThrownBy(() -> bookRequestService.createBookRequest(
                1L, new BookRequestCreateRequest("제목", "저자", "출판사", "이유")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_REQUEST_ALREADY_EXISTS);

        verify(bookRequestCandidateRepository, never()).upsertCandidate(any(), any(), any(), any());
    }

    @Test
    void createBookRequest_동시신청으로_UNIQUE위반이면_409로_변환한다() {
        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(bookRequestRepository.existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
                eq(1L), anyString(), anyString(), anyString())).willReturn(false);
        given(bookRequestCandidateRepository.lastInsertId()).willReturn(100L);
        given(bookRequestCandidateRepository.getReferenceById(100L))
                .willReturn(BookRequestCandidate.createNew("제목", "저자", "출판사", LocalDateTime.now()));
        given(bookRequestRepository.saveAndFlush(any(BookRequest.class)))
                .willThrow(new DataIntegrityViolationException(
                        "Duplicate entry '1-제목-저자-출판사' for key 'book_request.uk_book_request_member_normalized'"));

        assertThatThrownBy(() -> bookRequestService.createBookRequest(
                1L, new BookRequestCreateRequest("제목", "저자", "출판사", "이유")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_REQUEST_ALREADY_EXISTS);
    }

    @Test
    void createBookRequest_다른_제약위반이면_원래_예외를_그대로_던진다() {
        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(bookRequestRepository.existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
                eq(1L), anyString(), anyString(), anyString())).willReturn(false);
        given(bookRequestCandidateRepository.lastInsertId()).willReturn(100L);
        given(bookRequestCandidateRepository.getReferenceById(100L))
                .willReturn(BookRequestCandidate.createNew("제목", "저자", "출판사", LocalDateTime.now()));
        DataIntegrityViolationException unrelated =
                new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails");
        given(bookRequestRepository.saveAndFlush(any(BookRequest.class))).willThrow(unrelated);

        assertThatThrownBy(() -> bookRequestService.createBookRequest(
                1L, new BookRequestCreateRequest("제목", "저자", "출판사", "이유")))
                .isSameAs(unrelated);
    }

    @Test
    void getMyBookRequests_후보가_IN_REVIEW이고_신청이_REQUESTED이면_IN_REVIEW로_계산된다() {
        Member member = mock(Member.class);
        BookRequestCandidate candidate = mock(BookRequestCandidate.class);
        given(candidate.getCandidateStatus()).willReturn("IN_REVIEW");
        given(candidate.getRequestCount()).willReturn(2);

        BookRequest br = BookRequest.create(candidate, member, "제목", "저자", "출판사",
                "제목", "저자", "출판사", "사유", LocalDateTime.now());
        given(bookRequestRepository.findMyRequests(eq(1L), isNull(), any()))
                .willReturn(new PageImpl<>(List.of(br)));

        PageResponse<BookRequestHistoryItemResponse> response = bookRequestService.getMyBookRequests(1L, null, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("IN_REVIEW");
        assertThat(response.content().get(0).requestCount()).isEqualTo(2);
    }

    @Test
    void getMyBookRequests_유효하지_않은_status는_VALIDATION_ERROR() {
        assertThatThrownBy(() -> bookRequestService.getMyBookRequests(1L, "INVALID", null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
