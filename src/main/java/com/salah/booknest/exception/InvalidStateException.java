package com.salah.booknest.exception;

/** The request is valid but not allowed in the current state, e.g. approving a cancelled loan. Mapped to 409. */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}