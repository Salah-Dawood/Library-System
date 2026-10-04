package com.salah.booknest.security;

import io.jsonwebtoken.Claims;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class JWTUtils {

    Logger logger = Logger.getLogger(JWTUtils.class.getName());

    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expiration-ms}")
    private int jwtExpirationMs;


    public String generateJwtToken(MyUserDetails myUserDetails){
        return Jwts.builder()
                .setSubject(myUserDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256,jwtSecret)
                .compact();
    }

    public String getUserNameFromJwtToken(String token){
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.log(Level.SEVERE, "JWT validation failed: {0}", e.getMessage());
        }
        return false;
    }

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String RESET_PURPOSE = "password-reset";

    //verify if passwordtoken valid
    public boolean isPasswordResetToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody();
        return RESET_PURPOSE.equals(claims.get(PURPOSE_CLAIM));
    }

    public String generatePasswordResetToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim(PURPOSE_CLAIM, RESET_PURPOSE)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 900000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}
