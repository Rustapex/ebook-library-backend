package com.acorn.abc.rental.service;

import lombok.Getter;

@Getter
public class ProgressRevisionConflictException extends RuntimeException {

    private final long latestRevision;

    public ProgressRevisionConflictException(long latestRevision) {
        super("Progress revision conflict");
        this.latestRevision = latestRevision;
    }
}
