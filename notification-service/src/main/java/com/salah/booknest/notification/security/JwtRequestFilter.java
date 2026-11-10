package com.salah.booknest.notification.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Logs the caller in from the token alone: the user id, username and role are claims of the token.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;

    // the live notification stream finishes on a second (async) pass, which must be logged in too
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            Claims claims = jwtUtils.getLoginClaims(header.substring(7));
            if (claims != null && claims.get("userId") != null && claims.get("role") != null) {
                TokenUser user = new TokenUser(claims.get("userId", Long.class), claims.getSubject());
                var authority = new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, List.of(authority)));
            }
        }
        filterChain.doFilter(request, response);
    }
}
