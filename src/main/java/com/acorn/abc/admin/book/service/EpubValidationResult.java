package com.acorn.abc.admin.book.service;

public record EpubValidationResult(String safeFileName, long fileSize, String sha256) {
}
