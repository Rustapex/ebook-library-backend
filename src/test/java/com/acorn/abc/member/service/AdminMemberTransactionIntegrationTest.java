package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeRequest;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeResponse;
import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 관리자 회원 API 구현계획(2026-07-16) §1.4/§1.5/§1.6 — 실제 Spring 프록시 + MySQL로 트랜잭션/잠금/
 * 멱등성 경계를 검증한다. AdminCollectionTransactionIntegrationTest(PR50)와 동일한 원칙으로,
 * 테스트 메서드 자체에는 @Transactional을 붙이지 않는다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminMemberTransactionIntegrationTest {

    private static final String PREFIX = "ADMMEM_TX_";

    @Autowired
    private AdminMemberService adminMemberService;

    @Autowired
    private AdminPointAdjustService adminPointAdjustService;

    @Autowired
    private AccountSuspensionExpiryBatchService accountSuspensionExpiryBatchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember(PREFIX + UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() {
        cleanupMember(memberId);
    }

    @Test
    void changeMemberStatus_ACCOUNT_SUSPENSION이_상태_제재이력_감사로그를_함께_커밋한다() {
        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest(
                "SANCTIONED", "계정정지 테스트", "ACCOUNT_SUSPENSION", LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        AdminMemberStatusChangeResponse response = adminMemberService.changeMemberStatus(memberId, request, null);

        assertThat(response.status()).isEqualTo("SANCTIONED");
        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM member WHERE member_id = ?", String.class, memberId);
        Long currentSanctionId = jdbcTemplate.queryForObject(
                "SELECT current_sanction_id FROM member WHERE member_id = ?", Long.class, memberId);
        Long sanctionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sanction_history WHERE member_id = ? AND sanction_type = 'ACCOUNT_SUSPENSION'",
                Long.class, memberId);
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'MEMBER_UPDATE' AND target_id = ?",
                Long.class, memberId);

        assertThat(persistedStatus).isEqualTo("SANCTIONED");
        assertThat(currentSanctionId).isNotNull();
        assertThat(sanctionCount).isEqualTo(1L);
        assertThat(auditCount).isEqualTo(1L);
    }

    @Test
    void adjustPoint_같은_requestId를_동시에_두번_요청해도_이력과_잔액변경이_한번만_반영된다() throws Exception {
        String requestId = UUID.randomUUID().toString();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<Callable<AdminPointAdjustResponse>> tasks = List.of(
                () -> callAdjust(requestId, barrier),
                () -> callAdjust(requestId, barrier));
        List<Future<AdminPointAdjustResponse>> futures = tasks.stream().map(executor::submit).toList();
        executor.shutdown();
        assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();

        List<AdminPointAdjustResponse> responses = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        // 승자 응답은 메모리 상의 LocalDateTime.now()(나노초 포함), 패자의 replay 응답은 DB에 저장된
        // createdAt(초 단위)을 읽으므로 createdAt은 비교 대상에서 제외한다(DATETIME 컬럼 정밀도 차이일 뿐,
        // 잔액이 두 번 반영되지 않았다는 아래 DB 검증이 이 테스트의 핵심 단언이다).
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).pointHistoryId()).isEqualTo(responses.get(1).pointHistoryId());
        assertThat(responses.get(0).pointAmount()).isEqualTo(responses.get(1).pointAmount());
        assertThat(responses.get(0).pointBalance()).isEqualTo(responses.get(1).pointBalance());
        Long historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM point_history WHERE member_id = ? AND request_id = ?",
                Long.class, memberId, requestId);
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT point_balance FROM member WHERE member_id = ?", Integer.class, memberId);
        assertThat(historyCount).isEqualTo(1L);
        assertThat(balance).isEqualTo(1100);
    }

    private AdminPointAdjustResponse callAdjust(String requestId, CyclicBarrier barrier) throws Exception {
        barrier.await(10, TimeUnit.SECONDS);
        return adminPointAdjustService.adjustPoint(memberId, requestId, 100, "동시 지급", null);
    }

    @Test
    void restoreExpiredAccountSuspensions_만료된_계정정지를_복구하고_재실행해도_한번만_처리한다() {
        Long sanctionId = insertSanction(memberId, "ACCOUNT_SUSPENSION",
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        jdbcTemplate.update("UPDATE member SET status = 'SANCTIONED', current_sanction_id = ? WHERE member_id = ?",
                sanctionId, memberId);

        int firstRun = accountSuspensionExpiryBatchService.restoreExpiredAccountSuspensions();

        String statusAfterFirst = jdbcTemplate.queryForObject(
                "SELECT status FROM member WHERE member_id = ?", String.class, memberId);
        Long currentSanctionAfterFirst = jdbcTemplate.queryForObject(
                "SELECT current_sanction_id FROM member WHERE member_id = ?", Long.class, memberId);
        Long auditCountAfterFirst = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'MEMBER_UPDATE' AND target_id = ?",
                Long.class, memberId);

        assertThat(firstRun).isGreaterThanOrEqualTo(1);
        assertThat(statusAfterFirst).isEqualTo("JOINED");
        assertThat(currentSanctionAfterFirst).isNull();
        assertThat(auditCountAfterFirst).isEqualTo(1L);

        accountSuspensionExpiryBatchService.restoreExpiredAccountSuspensions();

        Long auditCountAfterSecond = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log WHERE action_type = 'MEMBER_UPDATE' AND target_id = ?",
                Long.class, memberId);
        assertThat(auditCountAfterSecond).isEqualTo(1L);
    }

    private Long insertMember(String loginId) {
        jdbcTemplate.update("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, " +
                        "point_balance, status, created_at) VALUES (?, 'hash', 'Tester', ?, '2000-01-01', 'USER', 1, 1000, 'JOINED', NOW())",
                loginId, loginId + "@test.com");
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE login_id = ?", Long.class, loginId);
    }

    private Long insertSanction(Long memberId, String sanctionType, LocalDateTime startedAt, LocalDateTime endedAt) {
        jdbcTemplate.update("INSERT INTO sanction_history (member_id, sanction_type, started_at, ended_at, created_at) " +
                "VALUES (?, ?, ?, ?, NOW())", memberId, sanctionType, startedAt, endedAt);
        return jdbcTemplate.queryForObject(
                "SELECT sanction_history_id FROM sanction_history WHERE member_id = ? ORDER BY sanction_history_id DESC LIMIT 1",
                Long.class, memberId);
    }

    private void cleanupMember(Long memberId) {
        if (memberId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM admin_audit_log WHERE target_type = 'MEMBER' AND target_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM point_history WHERE member_id = ?", memberId);
        jdbcTemplate.update("UPDATE member SET current_sanction_id = NULL WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM sanction_history WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM member WHERE member_id = ?", memberId);
    }

    /**
     * 감사 로그 저장 실패 시 상태/제재/포인트 변경도 함께 롤백되는지 확인한다.
     */
    @Nested
    @SpringBootTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    class AuditLogFailureRollback {

        private static final String RB_PREFIX = "ADMMEM_TX_RB_";

        @Autowired
        private AdminMemberService adminMemberService;

        @Autowired
        private AdminPointAdjustService adminPointAdjustService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @MockitoBean
        private AdminAuditLogWriter adminAuditLogWriter;

        private Long memberId;

        @BeforeEach
        void setUp() {
            doThrow(new RuntimeException("감사 로그 기록 실패(테스트 대역)"))
                    .when(adminAuditLogWriter).record(any(), any(), any(), any(), any(), any());
            jdbcTemplate.update("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, " +
                            "point_balance, status, created_at) VALUES (?, 'hash', 'Tester', ?, '2000-01-01', 'USER', 1, 1000, 'JOINED', NOW())",
                    RB_PREFIX + UUID.randomUUID(), RB_PREFIX + UUID.randomUUID() + "@test.com");
            memberId = jdbcTemplate.queryForObject(
                    "SELECT member_id FROM member WHERE login_id LIKE ? ORDER BY member_id DESC LIMIT 1", Long.class,
                    RB_PREFIX + "%");
        }

        @AfterEach
        void cleanUp() {
            if (memberId == null) {
                return;
            }
            jdbcTemplate.update("DELETE FROM point_history WHERE member_id = ?", memberId);
            jdbcTemplate.update("UPDATE member SET current_sanction_id = NULL WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM sanction_history WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM member WHERE member_id = ?", memberId);
        }

        @Test
        void changeMemberStatus_감사로그_실패시_상태와_제재이력이_남지_않는다() {
            AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest(
                    "SANCTIONED", "계정정지", "ACCOUNT_SUSPENSION", LocalDateTime.now(), LocalDateTime.now().plusDays(7));

            assertThatThrownBy(() -> adminMemberService.changeMemberStatus(memberId, request, null))
                    .isInstanceOf(RuntimeException.class);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM member WHERE member_id = ?", String.class, memberId);
            Long sanctionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sanction_history WHERE member_id = ?", Long.class, memberId);
            assertThat(status).isEqualTo("JOINED");
            assertThat(sanctionCount).isEqualTo(0L);
        }

        @Test
        void adjustPoint_감사로그_실패시_잔액과_이력이_남지_않는다() {
            String requestId = UUID.randomUUID().toString();

            assertThatThrownBy(() -> adminPointAdjustService.adjustPoint(memberId, requestId, 100, "지급", null))
                    .isInstanceOf(RuntimeException.class);

            Integer balance = jdbcTemplate.queryForObject(
                    "SELECT point_balance FROM member WHERE member_id = ?", Integer.class, memberId);
            Long historyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM point_history WHERE member_id = ?", Long.class, memberId);
            assertThat(balance).isEqualTo(1000);
            assertThat(historyCount).isEqualTo(0L);
        }
    }
}
