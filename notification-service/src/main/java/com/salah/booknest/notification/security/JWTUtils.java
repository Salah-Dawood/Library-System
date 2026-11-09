package com.salah.booknest.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads the tokens issued by user-service. This service never creates tokens.
 */
@Slf4j
@Service
public class JWTUtils {

    private static final String PURPOSE_CLAIM = "purpose";

    @Value("${jwt-secret}")
    private String jwtSecret;

    /** The claims of a valid login token, or null if the token is invalid, expired or a password-reset token. */
    public Claims getLoginClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody();
            return claims.get(PURPOSE_CLAIM) == null ? claims : null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }
}
