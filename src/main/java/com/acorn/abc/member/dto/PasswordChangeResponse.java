package com.acorn.abc.member.dto;

import java.time.LocalDateTime;

public record PasswordChangeResponse(
        LocalDateTime changedAt
) {}
