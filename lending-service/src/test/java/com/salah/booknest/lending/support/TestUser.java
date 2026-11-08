package com.salah.booknest.lending.support;

/** A user as the tests see it: a members row plus the role its token carries. */
public record TestUser(Long id, String username, String role) {
}
