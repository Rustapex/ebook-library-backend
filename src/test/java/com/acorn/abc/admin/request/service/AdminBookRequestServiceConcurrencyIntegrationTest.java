package com.acorn.abc.admin.request.service;

import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateRequest;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.request.dto.BookRequestCreateRequest;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.request.service.BookRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

/**
 * 실제 MySQL transaction/row lock 경계를 검증한다. 테스트 메서드에 @Transactional을 붙이지 않는다.
 * 실행 DB는 V002와 관리자 감사로그 CHECK sidecar가 적용된 disposable schema여야 한다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminBookRequestServiceConcurrencyIntegrationTest {

    private static final String PREFIX = "BR_CONC_";
    private static final long WAIT_SECONDS = 10;

    private final AdminBookRequestService adminBookRequestService;
    private final BookRequestService bookRequestService;
    private final JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private BookRequestCandidateRepository candidateRepository;

    private Long memberAId;
    private Long memberBId;
    private Long adminId;

    @Autowired
    AdminBookRequestServiceConcurrencyIntegrationTest(
            AdminBookRequestService adminBookRequestService,
            BookRequestService bookRequestService,
            JdbcTemplate jdbcTemplate
    ) {
        this.adminBookRequestService = adminBookRequestService;
        this.bookRequestService = bookRequestService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        memberAId = insertMember("user_a");
        memberBId = insertMember("user_b");
        adminId = insertMember("admin");
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE member_id = ?", adminId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM admin_audit_log WHERE target_type = 'BOOK_REQUEST_CANDIDATE' " +
                "AND target_id IN (SELECT request_candidate_id FROM book_request_candidate " +
                "WHERE normalized_title LIKE ?)", PREFIX.toLowerCase() + "%");
        jdbcTemplate.update("DELETE FROM notification WHERE target_type = 'BOOK_REQUEST_CANDIDATE' " +
                "AND target_id IN (SELECT request_candidate_id FROM book_request_candidate " +
                "WHERE normalized_title LIKE ?)", PREFIX.toLowerCase() + "%");
        jdbcTemplate.update("DELETE FROM book_request WHERE request_candidate_id IN " +
                "(SELECT request_candidate_id FROM book_request_candidate WHERE normalized_title LIKE ?)",
                PREFIX.toLowerCase() + "%");
        jdbcTemplate.update("DELETE FROM book_request_candidate WHERE normalized_title LIKE ?", PREFIX.toLowerCase() + "%");
        jdbcTemplate.update("DELETE FROM member WHERE member_id IN (?, ?, ?)", memberAId, memberBId, adminId);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void 신규신청과_종료처리가_동시에_실행돼도_종료된_신청자에게만_알림이_생긴다() throws Exception {
        String suffix = UUID.randomUUID().toString();
        BookRequestCreateRequest request = requestFor(suffix);
        BookRequestCreateResponse first = bookRequestService.createBookRequest(memberAId, request);

        CountDownLatch terminalBeforeCandidateUpdate = new CountDownLatch(1);
        CountDownLatch allowTerminalCandidateUpdate = new CountDownLatch(1);
        doAnswer(invocation -> {
            terminalBeforeCandidateUpdate.countDown();
            await(allowTerminalCandidateUpdate, "신규 신청이 commit된 뒤 종료 처리를 재개하지 못했습니다.");
            // Spring Data repository interface의 abstract method는 Mockito spy에서 callRealMethod()를
            // 호출할 수 없다. production JPQL과 같은 조건부 UPDATE를 현재 service transaction에
            // 참여하는 JdbcTemplate로 실행해, 이후 production 대상 조회/벌크 UPDATE/알림 흐름을 검증한다.
            Long candidateId = invocation.getArgument(0);
            String newStatus = invocation.getArgument(1);
            return jdbcTemplate.update("UPDATE book_request_candidate SET candidate_status = ? " +
                    "WHERE request_candidate_id = ? AND candidate_status IN ('REQUESTED', 'IN_REVIEW')",
                    newStatus, candidateId);
        }).when(candidateRepository).finalizeIfEligible(anyLong(), eq("REJECTED"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AdminBookRequestStatusUpdateResponse> terminal = executor.submit(() -> {
                return adminBookRequestService.updateStatus(adminId, first.requestCandidateId(),
                        new AdminBookRequestStatusUpdateRequest("REJECTED", null, "동시성 반려"));
            });

            // finalizeIfEligible 직전에서 terminal transaction을 멈춘다. 이 사이 B의 신청을
            // 완전히 commit시킨 뒤 candidate lock -> 대상 조회 순서를 실행해야 B도 알림 대상이 된다.
            await(terminalBeforeCandidateUpdate, "종료 처리가 candidate 조건부 UPDATE 직전까지 도달하지 못했습니다.");
            Future<BookRequestCreateResponse> newRequest = executor.submit(() ->
                    bookRequestService.createBookRequest(memberBId, request));
            assertThat(newRequest.get(WAIT_SECONDS, TimeUnit.SECONDS).requestCandidateId())
                    .isEqualTo(first.requestCandidateId());

            allowTerminalCandidateUpdate.countDown();
            assertThat(terminal.get(WAIT_SECONDS, TimeUnit.SECONDS).status()).isEqualTo("REJECTED");
        } finally {
            allowTerminalCandidateUpdate.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }

        Long candidateId = first.requestCandidateId();
        Long requestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM book_request WHERE request_candidate_id = ?", Long.class, candidateId);
        Long candidateRequestCount = jdbcTemplate.queryForObject(
                "SELECT request_count FROM book_request_candidate WHERE request_candidate_id = ?", Long.class, candidateId);
        List<Long> terminalMemberIds = jdbcTemplate.query(
                "SELECT member_id FROM book_request WHERE request_candidate_id = ? AND request_status = 'REJECTED'",
                (rs, rowNum) -> rs.getLong(1), candidateId);
        List<Long> notificationMemberIds = jdbcTemplate.query(
                "SELECT member_id FROM notification WHERE notification_type = 'BOOK_REQUEST' " +
                        "AND target_type = 'BOOK_REQUEST_CANDIDATE' AND target_id = ?",
                (rs, rowNum) -> rs.getLong(1), candidateId);

        assertThat(requestCount).isEqualTo(2L);
        assertThat(candidateRequestCount).isEqualTo(2L);
        assertThat(terminalMemberIds).containsExactlyInAnyOrder(memberAId, memberBId).doesNotHaveDuplicates();
        assertThat(notificationMemberIds).containsExactlyInAnyOrderElementsOf(terminalMemberIds).doesNotHaveDuplicates();
    }

    @Test
    void 같은회원의_동시_중복신청은_한건만_커밋되고_candidate증가도_롤백된다() throws Exception {
        BookRequestCreateRequest request = requestFor(UUID.randomUUID().toString());
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Attempt>> attempts = List.of(
                    executor.submit(createAttempt(memberAId, request, barrier)),
                    executor.submit(createAttempt(memberAId, request, barrier)));
            List<Attempt> results = List.of(
                    attempts.get(0).get(20, TimeUnit.SECONDS),
                    attempts.get(1).get(20, TimeUnit.SECONDS));

            assertThat(results.stream().filter(Attempt::succeeded).count()).isEqualTo(1);
            assertThat(results.stream().filter(result -> result.errorCode() == ErrorCode.BOOK_REQUEST_ALREADY_EXISTS).count())
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }

        Long requestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM book_request WHERE member_id = ? AND normalized_title LIKE ?",
                Long.class, memberAId, PREFIX.toLowerCase() + "%");
        Long candidateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM book_request_candidate WHERE normalized_title LIKE ?", Long.class, PREFIX.toLowerCase() + "%");
        Integer candidateRequestCount = jdbcTemplate.queryForObject(
                "SELECT request_count FROM book_request_candidate WHERE normalized_title LIKE ?", Integer.class,
                PREFIX.toLowerCase() + "%");

        assertThat(requestCount).isEqualTo(1L);
        assertThat(candidateCount).isEqualTo(1L);
        assertThat(candidateRequestCount).isEqualTo(1);
    }

    private Callable<Attempt> createAttempt(Long memberId, BookRequestCreateRequest request, CyclicBarrier barrier) {
        return () -> {
            barrier.await();
            try {
                bookRequestService.createBookRequest(memberId, request);
                return Attempt.success();
            } catch (BusinessException e) {
                return Attempt.failure(e.getErrorCode());
            }
        };
    }

    private void await(CountDownLatch latch, String failureMessage) {
        try {
            if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException(failureMessage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기 중 interrupt가 발생했습니다.", e);
        }
    }

    private BookRequestCreateRequest requestFor(String suffix) {
        String title = PREFIX + suffix;
        return new BookRequestCreateRequest(title, "동시성 저자", "동시성 출판사", "동시성 검증");
    }

    private Long insertMember(String label) {
        Long gradeId = jdbcTemplate.queryForObject("SELECT grade_id FROM member_grade ORDER BY grade_id LIMIT 1", Long.class);
        String unique = PREFIX + label + "_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, " +
                        "point_balance, status, created_at, updated_at) VALUES (?, 'test', ?, ?, '1990-01-01', 'USER', ?, 0, 'JOINED', NOW(), NOW())",
                unique, unique, unique + "@test.local", gradeId);
        return jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE login_id = ?", Long.class, unique);
    }

    private record Attempt(ErrorCode errorCode) {
        static Attempt success() {
            return new Attempt(null);
        }

        static Attempt failure(ErrorCode errorCode) {
            return new Attempt(errorCode);
        }

        boolean succeeded() {
            return errorCode == null;
        }
    }
}
