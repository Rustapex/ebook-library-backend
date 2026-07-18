package com.acorn.abc.admin.book.dto;

public record EpubUploadResponse(
        Long bookId,
        String fileName,
        long fileSize,
        String sha256,
        int epubVersion
) {
}
