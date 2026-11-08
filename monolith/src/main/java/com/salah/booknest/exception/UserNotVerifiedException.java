package com.salah.booknest.exception;

/**
 * Thrown when a user logs in with correct credentials but has not verified their email yet.
 */
public class UserNotVerifiedException extends RuntimeException {
    public UserNotVerifiedException(String message) {
        super(message);
    }
}
