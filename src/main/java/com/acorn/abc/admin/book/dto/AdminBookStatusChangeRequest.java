package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminBookStatusChangeRequest(

        @NotBlank(message = "상태는 필수입니다.")
        @Pattern(regexp = "^(AVAILABLE|HIDDEN|INACTIVE)$", message = "상태는 AVAILABLE, HIDDEN, INACTIVE 중 하나여야 합니다.")
        String status,

        // book 테이블에 사유 컬럼이 없어 감사 로그(admin_audit_log)에만 기록한다.
        @Size(max = 500, message = "사유는 500자 이하로 입력해주세요.")
        String reason
) {
}
