package com.salah.booknest.notification.cucumber;

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

    public String of(String username) {
        String role = username.equals("librarian") ? "librarian" : "Member";
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", Events.userId(username))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}
