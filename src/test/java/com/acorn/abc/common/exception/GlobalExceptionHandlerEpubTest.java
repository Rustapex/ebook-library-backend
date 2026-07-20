package com.acorn.abc.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerEpubTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingMultipartPart_returnsJson400Contract() {
        var response = handler.handleMissingServletRequestPartException(
                new MissingServletRequestPartException("file"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void malformedMultipart_returnsJson400Contract() {
        var response = handler.handleMultipartException(new MultipartException("malformed"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
    }
}
