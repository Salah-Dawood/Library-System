package com.salah.booknest.exception;

public class InvalidStateException extends RuntimeException {
    private final String code;

    public InvalidStateException(String message) {
        this(message, "INVALID_STATE");
    }

    public InvalidStateException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}