package com.acorn.abc.request.dto;

public record BookRequestHistoryCriteria(
        String status,
        int page,
        int size
) {
}
