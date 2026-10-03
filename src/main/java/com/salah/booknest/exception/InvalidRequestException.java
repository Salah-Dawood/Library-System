package com.salah.booknest.exception;

/** The request itself is wrong (missing or out-of-range values). Mapped to 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}