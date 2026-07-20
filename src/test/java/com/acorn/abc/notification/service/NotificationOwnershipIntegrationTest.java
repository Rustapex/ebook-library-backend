package com.acorn.abc.notification.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.notification.dto.NotificationReadResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * notification_id 단독이 아니라 (notification_id, member_id) 소유권으로 조회하는지 실제 DB에서 검증한다.
 * 서비스 transaction을 가리지 않도록 테스트 메서드에는 @Transactional을 붙이지 않는다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationOwnershipIntegrationTest {

    private static final String PREFIX = "NOTI_OWNER_";

    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    private Long ownerMemberId;
    private Long otherMemberId;
    private Long notificationId;

    @Autowired
    NotificationOwnershipIntegrationTest(NotificationService notificationService, JdbcTemplate jdbcTemplate) {
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        ownerMemberId = insertMember("owner");
        otherMemberId = insertMember("other");
        jdbcTemplate.update("INSERT INTO notification (member_id, notification_type, title, target_type, target_id, read_yn, created_at) " +
                        "VALUES (?, 'NOTICE', ?, 'NOTICE', 1, 0, NOW())", ownerMemberId, PREFIX + UUID.randomUUID());
        notificationId = jdbcTemplate.queryForObject(
                "SELECT notification_id FROM notification WHERE member_id = ? AND title LIKE ? ORDER BY notification_id DESC LIMIT 1",
                Long.class, ownerMemberId, PREFIX + "%");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification WHERE notification_id = ?", notificationId);
        jdbcTemplate.update("DELETE FROM member WHERE member_id IN (?, ?)", ownerMemberId, otherMemberId);
    }

    @Test
    void 타인알림은_404이고_본인알림은_두번_읽어도_동일한읽음시각을_유지한다() {
        assertThatThrownBy(() -> notificationService.markAsRead(otherMemberId, notificationId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

        Integer unreadAfterOtherAttempt = jdbcTemplate.queryForObject(
                "SELECT read_yn FROM notification WHERE notification_id = ?", Integer.class, notificationId);
        assertThat(unreadAfterOtherAttempt).isZero();

        NotificationReadResponse first = notificationService.markAsRead(ownerMemberId, notificationId);
        Timestamp persistedReadAt = jdbcTemplate.queryForObject(
                "SELECT read_at FROM notification WHERE notification_id = ?", Timestamp.class, notificationId);
        NotificationReadResponse second = notificationService.markAsRead(ownerMemberId, notificationId);

        assertThat(first.readYn()).isTrue();
        assertThat(first.readAt()).isNotNull();
        assertThat(persistedReadAt).isNotNull();
        assertThat(second.readYn()).isTrue();
        assertThat(second.readAt()).isEqualTo(persistedReadAt.toLocalDateTime());
    }

    private Long insertMember(String label) {
        Long gradeId = jdbcTemplate.queryForObject("SELECT grade_id FROM member_grade ORDER BY grade_id LIMIT 1", Long.class);
        String unique = PREFIX + label + "_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, " +
                        "point_balance, status, created_at, updated_at) VALUES (?, 'test', ?, ?, '1990-01-01', 'USER', ?, 0, 'JOINED', NOW(), NOW())",
                unique, unique, unique + "@test.local", gradeId);
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE login_id = ?", Long.class, unique);
    }
}
