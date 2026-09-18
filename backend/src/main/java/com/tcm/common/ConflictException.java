package com.tcm.common;

/**
 * Thrown when a request clashes with the current state of the system rather
 * than being invalid in itself - a double-booked trainer or classroom, say.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
