package com.acorn.abc.admin.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

// API-ADMIN-MEMBER-003 요청(api-spec 행 345). sanctionType이 있으면 SANCTION_HISTORY를 함께 생성한다(구현계획 §1.4).
// - sanctionType=ACCOUNT_SUSPENSION이면 status는 SANCTIONED여야 한다.
// - sanctionType=RENTAL_BAN/REVIEW_BAN이면 status는 JOINED로 유지되어야 한다(회원 상태는 바뀌지 않는다).
// - sanctionType이 없으면 status는 SANCTIONED를 제외한 JOINED/WITHDRAWN/DEACTIVATED 중 하나여야 한다
//   (SANCTIONED는 원인 제재 없이 성립할 수 없으므로 이 조합은 거부한다).
public record AdminMemberStatusChangeRequest(

        @NotBlank(message = "상태는 필수입니다.")
        @Pattern(regexp = "^(JOINED|SANCTIONED|WITHDRAWN|DEACTIVATED)$", message = "상태는 JOINED, SANCTIONED, WITHDRAWN, DEACTIVATED 중 하나여야 합니다.")
        String status,

        @NotBlank(message = "사유는 필수입니다.")
        String reason,

        @Pattern(regexp = "^(ACCOUNT_SUSPENSION|RENTAL_BAN|REVIEW_BAN)$", message = "제재 유형은 ACCOUNT_SUSPENSION, RENTAL_BAN, REVIEW_BAN 중 하나여야 합니다.")
        String sanctionType,

        LocalDateTime startedAt,

        LocalDateTime endedAt
) {
}
