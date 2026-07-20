package com.acorn.abc.notification.service;

import com.acorn.abc.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 알림 보관 주기 정리. 스케줄러 등록은 운영 주기 확정 후 별도로 진행한다(이번 범위 제외).
@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public int deleteNotificationsOlderThanOneMonth() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(1);
        return notificationRepository.deleteOlderThan(cutoff);
    }
}
