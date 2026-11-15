package com.salah.booknest.catalog.cucumber;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/** Login tokens like the ones user-service issues, signed with the test secret. */
@Component
public class Tokens {

    @Value("${jwt-secret}")
    private String jwtSecret;

    public String librarian() {
        return token(1L, "librarian", "librarian");
    }

    /** The sample members have the same ids as in user-service: johnd 2, janed 3, alexm 4. */
    public String member(String username) {
        long id = switch (username) {
            case "johnd" -> 2L;
            case "janed" -> 3L;
            case "alexm" -> 4L;
            default -> 99L;
        };
        return token(id, username, "Member");
    }

    private String token(long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}
