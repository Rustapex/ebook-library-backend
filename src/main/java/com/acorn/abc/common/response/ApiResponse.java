package com.acorn.abc.common.response;

import lombok.Getter;

/**
 * 모든 API 성공 응답에 사용하는 공통 래퍼 클래스.
 * Controller에서 직접 ResponseEntity를 반환할 때 body로 사용한다.
 *
 * [사용법]
 *
 * 1. 조회/수정/삭제 성공 (200 OK)
 *    return ResponseEntity.ok(ApiResponse.ok(data));
 *
 * 2. 생성 성공 (201 Created)
 *    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(savedId));
 *
 * 3. 응답 데이터가 없는 성공 (200 OK, data: null)
 *    return ResponseEntity.ok(ApiResponse.noContent());
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "CREATED", "생성되었습니다.", data);
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, "OK", "처리되었습니다.", null);
    }
}
