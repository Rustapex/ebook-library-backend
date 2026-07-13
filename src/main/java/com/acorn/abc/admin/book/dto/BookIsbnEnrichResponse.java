package com.acorn.abc.admin.book.dto;

import java.util.List;

public record BookIsbnEnrichResponse(
        int attemptedCount,
        int readyCount,
        List<String> failedIsbns
) {
}
