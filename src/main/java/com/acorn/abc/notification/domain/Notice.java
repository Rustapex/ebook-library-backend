package com.acorn.abc.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "notice_status", nullable = false, length = 20)
    private String noticeStatus;

    @Column(name = "notify_yn", nullable = false)
    private Boolean notifyYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Notice create(String title, String content, String noticeStatus, Boolean notifyYn, LocalDateTime createdAt) {
        Notice notice = new Notice();
        notice.title = title;
        notice.content = content;
        notice.noticeStatus = noticeStatus;
        notice.notifyYn = notifyYn;
        notice.createdAt = createdAt;
        return notice;
    }

    // notifyYn 필드는 값을 저장만 하고, 이 메서드 호출로는 알림을 재발송하지 않는다
    // (fan-out은 신규 등록 경로에서만 호출하는 별도 관심사).
    public void update(String title, String content, String noticeStatus, Boolean notifyYn, LocalDateTime updatedAt) {
        this.title = title;
        this.content = content;
        this.noticeStatus = noticeStatus;
        this.notifyYn = notifyYn;
        this.updatedAt = updatedAt;
    }
}
