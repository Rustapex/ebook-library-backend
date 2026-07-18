package com.acorn.abc.common.exception;

import com.acorn.abc.common.response.ErrorResponse;
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

    // PESSIMISTIC_WRITE 잠금 대기 초과·deadlock(관리자 회원 API 구현계획 §1.5-8, §1.6.2-5) — 재시도 없이 409로 응답한다.
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlePessimisticLockingFailureException(PessimisticLockingFailureException e) {
        log.warn("비관적 잠금 획득 실패", e);
        return ResponseEntity.status(ErrorCode.CONCURRENT_UPDATE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.CONCURRENT_UPDATE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
