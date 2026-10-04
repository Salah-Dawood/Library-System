package com.salah.booknest.security;

import org.springframework.security.core.Authentication;

public final class Roles {

    private static final String LIBRARIAN_AUTHORITY = "ROLE_librarian";

    private Roles() {
    }

    public static boolean isLibrarian(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> LIBRARIAN_AUTHORITY.equals(authority.getAuthority()));
    }
}