package com.acorn.abc.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V002가 적용된 disposable MySQL schema에서만 실행하는 post-apply migration 검증이다.
 * 기본 전체 test는 이 DB에 의존하지 않도록 RUN_MIGRATION_INTEGRATION_TEST=true일 때만 활성화한다.
 * V002 -> U002 -> V002 CLI roundtrip은 MySQL DDL implicit commit을 포함하므로 이 test가 아닌
 * 별도 disposable DB 실행 절차에서 검증한다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_MIGRATION_INTEGRATION_TEST", matches = "true")
class RequestNotificationNoticeMigrationIntegrationTest {

    private static final String CANDIDATE_NORMALIZED_INDEX = "idx_book_request_candidate_normalized";
    private static final String NOTIFICATION_TYPE_CHECK = "chk_notification_notification_type";
    private static final String AUDIT_ACTION_CHECK = "chk_admin_audit_log_action_type";
    private static final String AUDIT_TARGET_CHECK = "chk_admin_audit_log_target_type";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    RequestNotificationNoticeMigrationIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void V002_적용후_희망도서_알림_감사로그_정합성이_유지된다() {
        assertThat(count("""
                SELECT COUNT(*)
                FROM (
                    SELECT 1
                    FROM book_request_candidate
                    GROUP BY normalized_title, normalized_author, normalized_publisher
                    HAVING COUNT(*) > 1
                ) duplicated_candidate
                """))
                .as("normalized 후보 중복")
                .isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM book_request br
                LEFT JOIN book_request_candidate c ON c.request_candidate_id = br.request_candidate_id
                WHERE c.request_candidate_id IS NULL
                """))
                .as("book_request의 orphan candidate FK")
                .isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM book_request_candidate c
                LEFT JOIN (
                    SELECT request_candidate_id, COUNT(*) AS actual_count
                    FROM book_request
                    GROUP BY request_candidate_id
                ) actual ON actual.request_candidate_id = c.request_candidate_id
                WHERE c.request_count <> COALESCE(actual.actual_count, 0)
                """))
                .as("request_count와 실제 신청 건수의 불일치")
                .isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'book_request_candidate'
                  AND index_name = ?
                  AND non_unique = 0
                """, CANDIDATE_NORMALIZED_INDEX))
                .as("candidate normalized UNIQUE index")
                .isEqualTo(3L);

        assertThat(count("""
                SELECT COUNT(*)
                FROM notification
                WHERE notification_type NOT IN (
                    'RENTAL', 'PAYMENT', 'NOTICE', 'EVENT', 'REPORT', 'BOOK_REQUEST', 'COUPON', 'CHALLENGE'
                )
                """))
                .as("notification_type 대문자/허용값 위반")
                .isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM information_schema.check_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_name = ?
                  AND LOWER(check_clause) LIKE '%payment%'
                  AND LOWER(check_clause) LIKE '%book_request%'
                """, NOTIFICATION_TYPE_CHECK))
                .as("notification_type 대문자 CHECK")
                .isEqualTo(1L);

        assertThat(count("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                    'book_request_candidate_v002_backup',
                    'book_request_v002_backup_mapping',
                    'book_request_v002_migration_meta'
                )
                """))
                .as("V002 backup/meta table")
                .isEqualTo(3L);

        assertThat(count("""
                SELECT COUNT(*)
                FROM book_request_v002_migration_meta
                """))
                .as("V002 migration meta row")
                .isEqualTo(1L);

        long expectedAuditCheckCount = count("""
                SELECT action_check_existed + target_check_existed
                FROM book_request_v002_migration_meta
                """);

        assertThat(count("""
                SELECT COUNT(*)
                FROM book_request_v002_migration_meta m
                WHERE m.candidate_backup_row_count <> (
                          SELECT COUNT(*) FROM book_request_candidate_v002_backup
                      )
                   OR m.mapping_backup_row_count <> (
                          SELECT COUNT(*) FROM book_request_v002_backup_mapping
                      )
                """))
                .as("V002 meta와 backup row count의 불일치")
                .isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM information_schema.check_constraints
                WHERE constraint_schema = DATABASE()
                  AND (
                    (constraint_name = ? AND LOWER(check_clause) LIKE '%book_request_status_update%')
                    OR (constraint_name = ? AND LOWER(check_clause) LIKE '%book_request_candidate%')
                  )
                """, AUDIT_ACTION_CHECK, AUDIT_TARGET_CHECK))
                .as("V002 전 존재했던 감사 로그 CHECK의 희망도서 enum")
                .isEqualTo(expectedAuditCheckCount);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        if (value == null) {
            throw new IllegalStateException("migration 검증 count 쿼리가 null을 반환했습니다.");
        }
        return value;
    }
}
