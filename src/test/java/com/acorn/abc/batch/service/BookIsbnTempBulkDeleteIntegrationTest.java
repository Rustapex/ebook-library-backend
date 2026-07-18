package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnTempBulkDeleteRequest;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookIsbnTempBulkDeleteIntegrationTest {

    private static final long WAIT_SECONDS = 5;
    private static final long FUTURE_TIMEOUT_SECONDS = 15;

    @Autowired
    private BookIsbnTempService bookIsbnTempService;
    @MockitoSpyBean
    private BookIsbnTempRepository bookIsbnTempRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private final List<Long> createdTempIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void cleanUp() {
        if (createdTempIds.isEmpty()) {
            return;
        }
        newTransactionTemplate().executeWithoutResult(status -> entityManager.createQuery(
                        "DELETE FROM BookIsbnTemp t WHERE t.id IN :tempIds")
                .setParameter("tempIds", new ArrayList<>(createdTempIds))
                .executeUpdate());
        createdTempIds.clear();
    }

    @Test
    void bulkDelete_wrongAffectedCount_rollsBackRealDeleteThroughSpringProxy() {
        List<Long> tempIds = seedPendingTemps(2);
        doAnswer(invocation -> {
            List<Long> requestedIds = invocation.getArgument(0);
            byte statusCd = invocation.getArgument(1);
            int actuallyDeleted = entityManager.createQuery(
                            "DELETE FROM BookIsbnTemp t WHERE t.id IN :tempIds AND t.statusCd = :statusCd")
                    .setParameter("tempIds", requestedIds)
                    .setParameter("statusCd", statusCd)
                    .executeUpdate();
            assertThat(actuallyDeleted).isEqualTo(requestedIds.size());
            return actuallyDeleted - 1;
        }).when(bookIsbnTempRepository).deleteAllByIdInAndStatusCd(
                anyList(), eq(BookIsbnTemp.STATUS_PENDING));

        assertThatThrownBy(() -> bookIsbnTempService.bulkDelete(
                new BookIsbnTempBulkDeleteRequest(tempIds)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("요청 수와 일치하지 않습니다");

        List<BookIsbnTemp> afterRollback = newTransactionTemplate().execute(
                status -> bookIsbnTempRepository.findAllById(tempIds));
        assertThat(afterRollback).isNotNull();
        assertThat(afterRollback).extracting(BookIsbnTemp::getId)
                .containsExactlyInAnyOrderElementsOf(tempIds);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void bulkDelete_pessimisticLock_serializesConcurrentStatusUpdateWithoutPartialDelete() throws Exception {
        List<Long> tempIds = seedPendingTemps(2);
        CountDownLatch deleteLockAcquired = new CountDownLatch(1);
        CountDownLatch updateAboutToFlush = new CountDownLatch(1);
        AtomicLong deleteConnectionId = new AtomicLong();
        AtomicLong updateConnectionId = new AtomicLong();
        AtomicBoolean updateAttempted = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> deleteFuture = null;
        Future<Throwable> updateFuture = null;

        try {
            TransactionTemplate deleteTransaction = newTransactionTemplate();
            TransactionTemplate updateTransaction = newTransactionTemplate();

            deleteFuture = executor.submit(() -> deleteTransaction.executeWithoutResult(status -> {
                deleteConnectionId.set(currentConnectionId());
                List<BookIsbnTemp> locked = bookIsbnTempRepository.findAllByIdInForUpdate(tempIds);
                assertThat(locked).hasSize(2);
                deleteLockAcquired.countDown();
                await(updateAboutToFlush, "상태 변경 transaction이 flush 직전까지 도달하지 못했습니다.");

                int deleted = bookIsbnTempRepository.deleteAllByIdInAndStatusCd(
                        tempIds, BookIsbnTemp.STATUS_PENDING);
                assertThat(deleted).isEqualTo(2);
            }));

            updateFuture = executor.submit(() -> {
                try {
                    await(deleteLockAcquired, "삭제 transaction이 row lock을 획득하지 못했습니다.");
                    updateTransaction.executeWithoutResult(status -> {
                        updateConnectionId.set(currentConnectionId());
                        BookIsbnTemp target = bookIsbnTempRepository.findById(tempIds.get(0)).orElseThrow();
                        target.markReady();
                        updateAttempted.set(true);
                        updateAboutToFlush.countDown();
                        bookIsbnTempRepository.flush();
                    });
                    return null;
                } catch (Throwable failure) {
                    return failure;
                }
            });

            get(deleteFuture);
            Throwable updateFailure = get(updateFuture);

            assertThat(updateAttempted).isTrue();
            assertThat(deleteConnectionId.get()).isPositive();
            assertThat(updateConnectionId.get()).isPositive().isNotEqualTo(deleteConnectionId.get());

            List<BookIsbnTemp> remaining = newTransactionTemplate().execute(
                    status -> bookIsbnTempRepository.findAllById(tempIds));
            assertThat(remaining).isNotNull().isEmpty();
            assertThat(updateFailure).isNotNull().isInstanceOf(RuntimeException.class);
        } finally {
            deleteLockAcquired.countDown();
            updateAboutToFlush.countDown();
            if (deleteFuture != null) {
                deleteFuture.cancel(true);
            }
            if (updateFuture != null) {
                updateFuture.cancel(true);
            }
            executor.shutdownNow();
            assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<Long> seedPendingTemps(int count) {
        List<Long> tempIds = newTransactionTemplate().execute(status -> {
            List<BookIsbnTemp> temps = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                temps.add(BookIsbnTemp.create(uniqueIsbn13()));
            }
            return bookIsbnTempRepository.saveAllAndFlush(temps).stream()
                    .map(BookIsbnTemp::getId)
                    .sorted()
                    .toList();
        });
        assertThat(tempIds).isNotNull().hasSize(count);
        createdTempIds.addAll(tempIds);
        return tempIds;
    }

    private TransactionTemplate newTransactionTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setTimeout((int) FUTURE_TIMEOUT_SECONDS);
        return template;
    }

    private long currentConnectionId() {
        Long connectionId = jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class);
        if (connectionId == null) {
            throw new IllegalStateException("MySQL connection ID를 조회하지 못했습니다.");
        }
        return connectionId;
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

    private <T> T get(Future<T> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private String uniqueIsbn13() {
        long value = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L);
        return String.format("979%010d", value);
    }
}
