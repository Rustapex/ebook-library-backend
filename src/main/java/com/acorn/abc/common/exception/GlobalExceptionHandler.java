package com.acorn.abc.common.exception;

import com.acorn.abc.common.response.ErrorResponse;
import com.acorn.abc.rental.service.ProgressRevisionConflictException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 전역 예외 처리 핸들러.
 * 별도로 손댈 필요 없다. 예외는 throw만 하면 여기서 자동으로 처리된다.
 *
 * [처리 흐름]
 * BusinessException      -> ErrorCode에 정의된 HTTP 상태 + 에러 코드로 응답
 * @Valid 검증 실패        -> 400 Bad Request + VALIDATION_ERROR + 필드별 오류 목록
 * 그 외 모든 Exception   -> 500 Internal Server Error (서버 로그에 스택트레이스 기록)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, e.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBindingException(Exception e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    // 요청 본문이 없거나 JSON을 읽을 수 없는 경우(회원정보 수정 빈 본문 등)를 500이 아니라
    // 기존 검증 실패와 동일한 400 공통 JSON으로 응답한다(회원정보수정 보완계획 §5.1).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("요청 본문을 읽을 수 없습니다.", e);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.BOOK_EPUB_TOO_LARGE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.BOOK_EPUB_TOO_LARGE));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(ProgressRevisionConflictException.class)
    public ResponseEntity<ErrorResponse> handleProgressRevisionConflictException(
            ProgressRevisionConflictException e) {
        return ResponseEntity.status(409)
                .header("X-Latest-Revision", Long.toString(e.getLatestRevision()))
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
    }

    // PR64 재코드리뷰 후속수정(2026-07-22) §2.2 — 예외 메시지 문자열 비교가 아니라 cause 체인의
    // SQLException vendor error/SQLState로 실제 MySQL lock wait timeout·deadlock만 골라 409로 변환한다.
    // 그 외 PessimisticLockingFailureException(원인 불명 잠금 실패 등)은 동시성 오류로 오분류하지 않고
    // 기존 공통 미처리 예외 응답인 500으로 보낸다.
    private static final int MYSQL_LOCK_WAIT_TIMEOUT_VENDOR_CODE = 1205;
    private static final String MYSQL_LOCK_WAIT_TIMEOUT_SQL_STATE = "HY000";
    private static final int MYSQL_DEADLOCK_VENDOR_CODE = 1213;
    private static final String MYSQL_DEADLOCK_SQL_STATE = "40001";

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlePessimisticLockingFailureException(PessimisticLockingFailureException e) {
        if (!isMySqlLockConflict(e)) {
            log.error("MySQL lock wait timeout/deadlock으로 판별되지 않는 PessimisticLockingFailureException", e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        log.warn("MySQL 잠금 충돌(lock wait timeout/deadlock)", e);
        return ResponseEntity.status(ErrorCode.CONCURRENT_UPDATE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.CONCURRENT_UPDATE));
    }

    // cause 체인을 순회하며 SQLException의 vendor error/SQLState를 확인한다. 순환 참조에 대비해
    // 이미 방문한 예외는 다시 방문하지 않는다(무한 루프 방지).
    private boolean isMySqlLockConflict(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            if (current instanceof SQLException sqlException) {
                boolean lockWaitTimeout = sqlException.getErrorCode() == MYSQL_LOCK_WAIT_TIMEOUT_VENDOR_CODE
                        && MYSQL_LOCK_WAIT_TIMEOUT_SQL_STATE.equals(sqlException.getSQLState());
                boolean deadlock = sqlException.getErrorCode() == MYSQL_DEADLOCK_VENDOR_CODE
                        && MYSQL_DEADLOCK_SQL_STATE.equals(sqlException.getSQLState());
                if (lockWaitTimeout || deadlock) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
