package com.acorn.abc.admin.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminNoticeSaveRequest(
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
        @NotBlank(message = "내용은 필수입니다.") String content,
        @NotBlank(message = "상태는 필수입니다.") String status,
        @NotNull(message = "알림 발송 여부는 필수입니다.") Boolean notifyYn
) {
}
