package com.acorn.abc.notification.repository;

import com.acorn.abc.notification.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByNoticeStatusOrderByCreatedAtDescNoticeIdDesc(String noticeStatus, Pageable pageable);

    Optional<Notice> findByNoticeIdAndNoticeStatus(Long noticeId, String noticeStatus);

    // prevNotice = 더 최신 ACTIVE 공지(현재보다 (createdAt, noticeId)가 큰 것 중 가장 가까운 것).
    @Query(value = """
            SELECT * FROM notice
            WHERE notice_status = 'ACTIVE'
              AND (created_at, notice_id) > (:createdAt, :noticeId)
            ORDER BY created_at ASC, notice_id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Notice> findPrevActive(@Param("createdAt") LocalDateTime createdAt, @Param("noticeId") Long noticeId);

    // nextNotice = 더 오래된 ACTIVE 공지(현재보다 작은 것 중 가장 가까운 것).
    @Query(value = """
            SELECT * FROM notice
            WHERE notice_status = 'ACTIVE'
              AND (created_at, notice_id) < (:createdAt, :noticeId)
            ORDER BY created_at DESC, notice_id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Notice> findNextActive(@Param("createdAt") LocalDateTime createdAt, @Param("noticeId") Long noticeId);

    @Query("SELECT n FROM Notice n WHERE (:status IS NULL OR n.noticeStatus = :status) ORDER BY n.createdAt DESC, n.noticeId DESC")
    Page<Notice> searchForAdmin(@Param("status") String status, Pageable pageable);
}
