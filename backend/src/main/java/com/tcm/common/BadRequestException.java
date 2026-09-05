package com.tcm.common;

/**
 * Thrown when a request is well-formed but semantically invalid. Mapped to
 * HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
