package com.acorn.abc.notification.repository;

import com.acorn.abc.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 마이페이지 미읽은 알림 수(PR #48 코드리뷰 대응 §3.2)
    long countByMember_MemberIdAndReadYnFalse(Long memberId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.member.memberId = :memberId
              AND (:readYn IS NULL OR n.readYn = :readYn)
              AND (:notificationType IS NULL OR n.notificationType = :notificationType)
            ORDER BY n.createdAt DESC, n.notificationId DESC
            """)
    Page<Notification> findMyNotifications(@Param("memberId") Long memberId,
                                            @Param("readYn") Boolean readYn,
                                            @Param("notificationType") String notificationType,
                                            Pageable pageable);

    Optional<Notification> findByNotificationIdAndMember_MemberId(Long notificationId, Long memberId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
