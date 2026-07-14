package com.acorn.abc.notification.repository;

import com.acorn.abc.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 마이페이지 미읽은 알림 수(PR #48 코드리뷰 대응 §3.2)
    long countByMember_MemberIdAndReadYnFalse(Long memberId);
}
