package com.acorn.abc.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외 클래스.
 * Service 계층에서 비즈니스 규칙 위반 시 던진다.
 * GlobalExceptionHandler가 잡아서 자동으로 에러 응답으로 변환한다.
 *
 * [사용법]
 * throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
 * throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
 *
 * [주의]
 * catch 후 재던지거나 직접 ResponseEntity를 만들지 않는다.
 * 그냥 throw 한 줄로 끝낸다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
