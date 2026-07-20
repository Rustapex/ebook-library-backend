package com.acorn.abc.notification.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.notification.domain.Notification;
import com.acorn.abc.notification.dto.NotificationItemResponse;
import com.acorn.abc.notification.dto.NotificationReadResponse;
import com.acorn.abc.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PageResponse<NotificationItemResponse> getMyNotifications(
            Long memberId, Boolean readYn, String notificationType, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<Notification> notifications =
                notificationRepository.findMyNotifications(memberId, readYn, notificationType, pageable);
        List<NotificationItemResponse> content = notifications.getContent().stream()
                .map(NotificationItemResponse::from)
                .toList();
        return PageResponse.of(content, notifications.getNumber(), notifications.getSize(), notifications.getTotalElements());
    }

    public NotificationReadResponse markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findByNotificationIdAndMember_MemberId(notificationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!Boolean.TRUE.equals(notification.getReadYn())) {
            notification.markAsRead(LocalDateTime.now());
        }
        return NotificationReadResponse.from(notification);
    }

    // 희망도서/공지 도메인에서 재사용하는 내부 알림 생성 메서드.
    public void notifyMember(Member member, String notificationType, String title, String targetType, Long targetId) {
        notificationRepository.save(Notification.create(member, notificationType, title, targetType, targetId, LocalDateTime.now()));
    }

    public void notifyMembers(List<Member> members, String notificationType, String title, String targetType, Long targetId) {
        LocalDateTime now = LocalDateTime.now();
        members.forEach(member ->
                notificationRepository.save(Notification.create(member, notificationType, title, targetType, targetId, now)));
    }

    // 공지 fan-out처럼 대량 회원에게 알림을 보낼 때 사용. Notification이 IDENTITY 채번이라
    // JPA saveAll은 실제 배치 insert가 되지 않으므로 JdbcTemplate.batchUpdate로 직접 처리한다.
    public void notifyMemberIdsBatch(List<Long> memberIds, String notificationType, String title, String targetType, Long targetId) {
        if (memberIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO notification (member_id, notification_type, title, target_type, target_id, read_yn, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 0, ?)";
        jdbcTemplate.batchUpdate(sql, memberIds, memberIds.size(), (ps, memberId) -> {
            ps.setLong(1, memberId);
            ps.setString(2, notificationType);
            ps.setString(3, title);
            ps.setString(4, targetType);
            if (targetId != null) {
                ps.setLong(5, targetId);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
        });
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
