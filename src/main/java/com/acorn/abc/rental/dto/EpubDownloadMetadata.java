package com.acorn.abc.rental.dto;

public record EpubDownloadMetadata(
        Long bookId,
        int epubVersion,
        String fileName,
        long fileSize,
        String sha256
) {
}
