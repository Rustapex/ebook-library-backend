package com.acorn.abc.book.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * 관리자 도서 수정/상태변경(API-ADMIN-BOOK-003/004)의 동시 수정 잠금 대기 시간을 트랜잭션 범위의
 * 동일 JDBC connection에서만 5초로 낮췄다가, 작업이 끝나면 원래 값으로 되돌린다
 * (관리자 도서 API 설계문서 §6, 계획 §3.3 원문 그대로 구현하기로 확정).
 * 이 코드베이스의 다른 PESSIMISTIC_WRITE 사용처(컬렉션/회원/제재이력)는 MySQL 기본
 * innodb_lock_wait_timeout(수십 초)을 그대로 쓰므로, 이 클래스는 도서 write 경로에서만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BookLockTimeoutSupport {

    private static final int LOCK_WAIT_TIMEOUT_SECONDS = 5;

    private final EntityManager entityManager;

    <T> T withShortLockTimeout(Supplier<T> action) {
        long[] originalTimeout = new long[1];
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT @@SESSION.innodb_lock_wait_timeout")) {
                    resultSet.next();
                    originalTimeout[0] = resultSet.getLong(1);
                }
                statement.execute("SET SESSION innodb_lock_wait_timeout = " + LOCK_WAIT_TIMEOUT_SECONDS);
            }
        });
        try {
            return action.get();
        } finally {
            restoreTimeout(originalTimeout[0]);
        }
    }

    // commit·rollback·예외 어느 경로든 여기까지 도달해 원래 값을 복원한다. 복원 자체가 실패하면
    // 커넥션이 이미 비정상 상태라는 뜻이므로 원 예외를 가리지 않고 로그만 남긴다.
    private void restoreTimeout(long originalTimeout) {
        try {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET SESSION innodb_lock_wait_timeout = " + originalTimeout);
                }
            });
        } catch (RuntimeException e) {
            log.warn("innodb_lock_wait_timeout 복원 실패", e);
        }
    }
}
