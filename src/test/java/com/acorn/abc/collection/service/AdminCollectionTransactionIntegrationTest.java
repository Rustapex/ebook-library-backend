package com.acorn.abc.collection.service;

import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveResponse;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * PR50 코드리뷰 HIGH-1 반영 — {@code AdminCollectionServiceTest}는 {@code new AdminCollectionService(...)}로
 * 직접 생성해 @Transactional이 적용되지 않으므로 findByIdForUpdate()의 PESSIMISTIC_WRITE lock이
 * 실제 트랜잭션 경계를 통과하는지, 엔티티 변경이 실제로 commit되는지 증명하지 못한다.
 * 이 클래스는 실제 Spring이 관리하는 빈(프록시)을 그대로 호출해 그 경계를 재현한다.
 * 테스트 메서드 자체에는 @Transactional을 붙이지 않는다 — 붙이면 서비스의 트랜잭션 선언
 * 누락을 테스트 트랜잭션이 가려버리기 때문이다(BookApprovalServiceRequiresNewIntegrationTest와
 * 동일한 원칙).
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminCollectionTransactionIntegrationTest {

    private static final String NAME_PREFIX = "PR50_TX_";

    @Autowired
    private AdminCollectionService adminCollectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long publisherId;
    private Long bookId;

    @BeforeEach
    void setUp() {
        publisherId = jdbcTemplate.queryForObject("SELECT publisher_id FROM publisher LIMIT 1", Long.class);
        String title = NAME_PREFIX + "도서_" + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO book (publisher_id, title, rental_type, rental_price, default_rental_days, " +
                        "total_pages, rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, ?, 'FREE', 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', NOW())",
                publisherId, title);
        bookId = jdbcTemplate.queryForObject("SELECT book_id FROM book WHERE title = ?", Long.class, title);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM admin_audit_log WHERE target_type = 'COLLECTION' AND target_id IN " +
                "(SELECT collection_id FROM collection WHERE name LIKE ?)", NAME_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM collection_book WHERE collection_id IN " +
                "(SELECT collection_id FROM collection WHERE name LIKE ?)", NAME_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM collection WHERE name LIKE ?", NAME_PREFIX + "%");
        if (bookId != null) {
            jdbcTemplate.update("DELETE FROM book WHERE book_id = ?", bookId);
        }
    }

    @Test
    void createAdminCollection_실제_트랜잭션에서_컬렉션과_감사로그가_함께_커밋된다() {
        String name = NAME_PREFIX + "생성_" + UUID.randomUUID();
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                name, "SERIES", null, null, null, null, "ACTIVE");

        AdminCollectionSaveResponse response = adminCollectionService.createAdminCollection(request, null);

        Long persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collection WHERE collection_id = ?", Long.class, response.collectionId());
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'COLLECTION_CREATE' AND target_id = ?",
                Long.class, response.collectionId());
        assertThat(persisted).isEqualTo(1L);
        assertThat(auditCount).isEqualTo(1L);
    }

    @Test
    void updateAdminCollection_잠금_조회_이후_변경사항이_실제로_커밋된다() {
        Long collectionId = insertCollection(NAME_PREFIX + "수정_" + UUID.randomUUID(), "SERIES", "ACTIVE");
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                NAME_PREFIX + "수정됨_" + UUID.randomUUID(), "SERIES", null, null, null, null, "HIDDEN");

        adminCollectionService.updateAdminCollection(collectionId, request, null);

        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM collection WHERE collection_id = ?", String.class, collectionId);
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'COLLECTION_UPDATE' AND target_id = ?",
                Long.class, collectionId);
        assertThat(persistedStatus).isEqualTo("HIDDEN");
        assertThat(auditCount).isEqualTo(1L);
    }

    @Test
    void upsertCollectionBooks_잠금_조회와_매핑_저장이_함께_커밋된다() {
        Long collectionId = insertCollection(NAME_PREFIX + "도서_" + UUID.randomUUID(), "SERIES", "ACTIVE");
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(
                List.of(new AdminCollectionBooksUpdateRequest.BookItem(bookId, 1)));

        adminCollectionService.upsertCollectionBooks(collectionId, request, null);

        Long mappedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collection_book WHERE collection_id = ? AND book_id = ?",
                Long.class, collectionId, bookId);
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'COLLECTION_BOOKS_UPDATE' AND target_id = ?",
                Long.class, collectionId);
        assertThat(mappedCount).isEqualTo(1L);
        assertThat(auditCount).isEqualTo(1L);
    }

    @Test
    void patchCollection_잠금_상태변경_매핑삭제_감사로그가_함께_커밋된다() {
        Long collectionId = insertCollection(NAME_PREFIX + "패치_" + UUID.randomUUID(), "SERIES", "ACTIVE");
        insertCollectionBook(collectionId, bookId, 1);
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest("HIDDEN", List.of(bookId));

        adminCollectionService.patchCollection(collectionId, request, null);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM collection WHERE collection_id = ?", String.class, collectionId);
        Long mappedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collection_book WHERE collection_id = ?", Long.class, collectionId);
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'COLLECTION_PATCH' AND target_id = ?",
                Long.class, collectionId);
        assertThat(status).isEqualTo("HIDDEN");
        assertThat(mappedCount).isEqualTo(0L);
        assertThat(auditCount).isEqualTo(1L);
    }

    private Long insertCollection(String name, String type, String status) {
        jdbcTemplate.update(
                "INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, ?, ?, NOW())",
                name, type, status);
        return jdbcTemplate.queryForObject("SELECT collection_id FROM collection WHERE name = ?", Long.class, name);
    }

    private void insertCollectionBook(Long collectionId, Long bookId, Integer displayOrder) {
        jdbcTemplate.update(
                "INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES (?, ?, ?, NOW())",
                collectionId, bookId, displayOrder);
    }

    /**
     * 감사 로그 저장 실패 시 도메인 변경도 함께 롤백되는지 확인한다. {@link AdminAuditLogWriter}를
     * 예외를 던지는 Mockito 대역으로 교체하므로 이 중첩 클래스는 별도 ApplicationContext로 캐시된다.
     */
    @Nested
    @SpringBootTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    class AuditLogFailureRollback {

        private static final String RB_PREFIX = "PR50_TX_RB_";

        @Autowired
        private AdminCollectionService adminCollectionService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @MockitoBean
        private AdminAuditLogWriter adminAuditLogWriter;

        private Long publisherId;
        private Long bookId;

        @BeforeEach
        void setUp() {
            doThrow(new RuntimeException("감사 로그 기록 실패(테스트 대역)"))
                    .when(adminAuditLogWriter).record(any(), any(), any(), any(), any(), any());

            publisherId = jdbcTemplate.queryForObject("SELECT publisher_id FROM publisher LIMIT 1", Long.class);
            String title = RB_PREFIX + "도서_" + UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO book (publisher_id, title, rental_type, rental_price, default_rental_days, " +
                            "total_pages, rental_count, average_rating, review_count, status, created_at) " +
                            "VALUES (?, ?, 'FREE', 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', NOW())",
                    publisherId, title);
            bookId = jdbcTemplate.queryForObject("SELECT book_id FROM book WHERE title = ?", Long.class, title);
        }

        @AfterEach
        void cleanUp() {
            jdbcTemplate.update("DELETE FROM collection_book WHERE collection_id IN " +
                    "(SELECT collection_id FROM collection WHERE name LIKE ?)", RB_PREFIX + "%");
            jdbcTemplate.update("DELETE FROM collection WHERE name LIKE ?", RB_PREFIX + "%");
            if (bookId != null) {
                jdbcTemplate.update("DELETE FROM book WHERE book_id = ?", bookId);
            }
        }

        @Test
        void createAdminCollection_감사로그_실패시_생성_row가_남지_않는다() {
            String name = RB_PREFIX + "생성_" + UUID.randomUUID();
            AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                    name, "SERIES", null, null, null, null, "ACTIVE");

            assertThatThrownBy(() -> adminCollectionService.createAdminCollection(request, null))
                    .isInstanceOf(RuntimeException.class);

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM collection WHERE name = ?", Long.class, name);
            assertThat(count).isEqualTo(0L);
        }

        @Test
        void updateAdminCollection_감사로그_실패시_수정_전_상태로_남는다() {
            String originalName = RB_PREFIX + "수정전_" + UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, 'SERIES', 'ACTIVE', NOW())",
                    originalName);
            Long collectionId = jdbcTemplate.queryForObject(
                    "SELECT collection_id FROM collection WHERE name = ?", Long.class, originalName);
            AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                    RB_PREFIX + "수정후", "SERIES", null, null, null, null, "HIDDEN");

            assertThatThrownBy(() -> adminCollectionService.updateAdminCollection(collectionId, request, null))
                    .isInstanceOf(RuntimeException.class);

            String persistedName = jdbcTemplate.queryForObject(
                    "SELECT name FROM collection WHERE collection_id = ?", String.class, collectionId);
            String persistedStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM collection WHERE collection_id = ?", String.class, collectionId);
            assertThat(persistedName).isEqualTo(originalName);
            assertThat(persistedStatus).isEqualTo("ACTIVE");
        }

        @Test
        void upsertCollectionBooks_감사로그_실패시_매핑이_남지_않는다() {
            String name = RB_PREFIX + "도서_" + UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, 'SERIES', 'ACTIVE', NOW())",
                    name);
            Long collectionId = jdbcTemplate.queryForObject(
                    "SELECT collection_id FROM collection WHERE name = ?", Long.class, name);
            AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(
                    List.of(new AdminCollectionBooksUpdateRequest.BookItem(bookId, 1)));

            assertThatThrownBy(() -> adminCollectionService.upsertCollectionBooks(collectionId, request, null))
                    .isInstanceOf(RuntimeException.class);

            Long mappedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM collection_book WHERE collection_id = ?", Long.class, collectionId);
            assertThat(mappedCount).isEqualTo(0L);
        }

        @Test
        void patchCollection_감사로그_실패시_상태와_매핑이_변경_전으로_남는다() {
            String name = RB_PREFIX + "패치_" + UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO collection (name, collection_type, status, created_at) VALUES (?, 'SERIES', 'ACTIVE', NOW())",
                    name);
            Long collectionId = jdbcTemplate.queryForObject(
                    "SELECT collection_id FROM collection WHERE name = ?", Long.class, name);
            jdbcTemplate.update(
                    "INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES (?, ?, 1, NOW())",
                    collectionId, bookId);
            AdminCollectionPatchRequest request = new AdminCollectionPatchRequest("HIDDEN", List.of(bookId));

            assertThatThrownBy(() -> adminCollectionService.patchCollection(collectionId, request, null))
                    .isInstanceOf(RuntimeException.class);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM collection WHERE collection_id = ?", String.class, collectionId);
            Long mappedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM collection_book WHERE collection_id = ?", Long.class, collectionId);
            assertThat(status).isEqualTo("ACTIVE");
            assertThat(mappedCount).isEqualTo(1L);
        }
    }
}
