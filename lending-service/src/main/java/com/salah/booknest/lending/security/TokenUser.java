package com.salah.booknest.lending.security;

import java.security.Principal;

/**
 * The logged-in user as described by the token. getName() is the username, like in the other services.
 */
public record TokenUser(Long id, String username) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
