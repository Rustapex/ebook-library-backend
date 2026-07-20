package com.acorn.abc.notification.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.notification.domain.Notification;
import com.acorn.abc.notification.dto.NotificationReadResponse;
import com.acorn.abc.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markAsRead_존재하지_않으면_NOTIFICATION_NOT_FOUND를_던진다() {
        given(notificationRepository.findByNotificationIdAndMember_MemberId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void markAsRead_안읽은_알림이면_읽음처리한다() {
        Member member = mock(Member.class);
        Notification notification = Notification.create(member, "NOTICE", "제목", "NOTICE", 1L, LocalDateTime.now());
        given(notificationRepository.findByNotificationIdAndMember_MemberId(1L, 2L)).willReturn(Optional.of(notification));

        NotificationReadResponse response = notificationService.markAsRead(2L, 1L);

        assertThat(response.readYn()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void markAsRead_이미_읽은_알림이면_그대로_반환하고_재변경하지_않는다() {
        Member member = mock(Member.class);
        Notification notification = Notification.create(member, "NOTICE", "제목", "NOTICE", 1L, LocalDateTime.now());
        LocalDateTime firstReadAt = LocalDateTime.now().minusDays(1);
        notification.markAsRead(firstReadAt);
        given(notificationRepository.findByNotificationIdAndMember_MemberId(1L, 2L)).willReturn(Optional.of(notification));

        NotificationReadResponse response = notificationService.markAsRead(2L, 1L);

        assertThat(response.readYn()).isTrue();
        assertThat(response.readAt()).isEqualTo(firstReadAt);
    }

    @Test
    void notifyMemberIdsBatch_빈_목록이면_batchUpdate를_호출하지_않는다() {
        notificationService.notifyMemberIdsBatch(java.util.List.of(), "NOTICE", "제목", "NOTICE", 1L);

        verify(jdbcTemplate, never()).batchUpdate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any());
    }
}
