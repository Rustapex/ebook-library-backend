package com.acorn.abc.admin.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// approvedBookId/rejectReason은 status에 따라 필수/금지가 갈리므로 필드 레벨에는
// null을 허용하는 완화된 제약만 둔다. 필수/금지/공백 판단은 서비스 레벨에서 처리한다.
public record AdminBookRequestStatusUpdateRequest(
        @NotBlank(message = "status는 필수입니다.") String status,
        Long approvedBookId,
        @Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.") String rejectReason
) {
}
