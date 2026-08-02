package com.acorn.abc.common.response;

import com.acorn.abc.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.validation.FieldError;

import java.util.Collections;
import java.util.List;

/**
 * 모든 API 에러 응답에 사용하는 공통 래퍼 클래스.
 * GlobalExceptionHandler에서 자동으로 사용하므로 직접 호출할 일은 거의 없다.
 *
 * [응답 형태]
 *
 * 1. 비즈니스 예외 (예: 이메일 중복, 리소스 없음)
 *    {
 *      "success": false,
 *      "code": "MEMBER_EMAIL_DUPLICATED",
 *      "message": "이미 사용 중인 이메일입니다.",
 *      "errors": []
 *    }
 *
 * 2. Validation 실패 (@Valid 검증 오류)
 *    {
 *      "success": false,
 *      "code": "VALIDATION_ERROR",
 *      "message": "입력값을 확인해주세요.",
 *      "errors": [
 *        { "field": "email", "message": "이메일 형식이 올바르지 않습니다." }
 *      ]
 *    }
 */
@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> errors;

    private ErrorResponse(String code, String message, List<FieldErrorDetail> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), Collections.emptyList());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        List<FieldErrorDetail> details = fieldErrors.stream()
                .map(e -> new FieldErrorDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details);
    }

    @Getter
    public static class FieldErrorDetail {
        private final String field;
        private final String message;

        public FieldErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
