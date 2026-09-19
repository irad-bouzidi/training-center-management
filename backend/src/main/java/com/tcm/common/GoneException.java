package com.tcm.common;

/**
 * Thrown when something that did exist has passed out of use - an expired QR
 * token, say. Mapped to HTTP 410 by {@link GlobalExceptionHandler}, which
 * says "this was valid, and isn't any more" where a 400 would only say "this
 * is wrong".
 */
public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
