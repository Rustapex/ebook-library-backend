package com.acorn.abc.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberWithdrawRequest(

        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @Size(max = 255, message = "탈퇴 사유는 255자 이하로 입력해주세요.")
        String withdrawalReason
) {}
