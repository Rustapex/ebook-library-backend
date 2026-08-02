package com.acorn.abc.admin.book.dto;

import java.util.List;

public record BookIsbnTempBulkDeleteResponse(
        int deletedCount,
        List<Long> deletedTempIds
) {
}
