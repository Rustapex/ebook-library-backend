package com.acorn.abc.notification.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.notification.domain.Notice;
import com.acorn.abc.notification.dto.NoticeDetailResponse;
import com.acorn.abc.notification.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void getNoticeDetail_ACTIVE가_아니면_NOTICE_NOT_FOUND를_던진다() {
        given(noticeRepository.findByNoticeIdAndNoticeStatus(1L, "ACTIVE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNoticeDetail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    void getNoticeDetail_이전다음_공지가_없으면_null을_반환한다() {
        Notice notice = Notice.create("제목", "내용", "ACTIVE", false, LocalDateTime.now());
        given(noticeRepository.findByNoticeIdAndNoticeStatus(1L, "ACTIVE")).willReturn(Optional.of(notice));
        given(noticeRepository.findPrevActive(any(), any())).willReturn(Optional.empty());
        given(noticeRepository.findNextActive(any(), any())).willReturn(Optional.empty());

        NoticeDetailResponse response = noticeService.getNoticeDetail(1L);

        assertThat(response.prevNotice()).isNull();
        assertThat(response.nextNotice()).isNull();
        assertThat(response.title()).isEqualTo("제목");
    }
}
