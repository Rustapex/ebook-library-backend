package com.acorn.abc.member.dto;

import java.time.LocalDateTime;

public record MemberWithdrawResponse(
        String status,
        LocalDateTime withdrawnAt
) {}
