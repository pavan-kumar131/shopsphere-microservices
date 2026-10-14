package com.shopsphere.notification;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAccessFilter extends OncePerRequestFilter {
    private final SecretKey key;
    public JwtAccessFilter(@Value("${security.jwt.secret}") String secret) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        key = Keys.hmacShaKeyFor(decoded);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/actuator/health") || request.getRequestURI().startsWith("/actuator/health/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) throw new IllegalArgumentException("Missing bearer token");
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
            if (claims.getSubject() == null || claims.getExpiration() == null || claims.getExpiration().getTime() <= System.currentTimeMillis() || claims.get("role", String.class) == null) throw new IllegalArgumentException("Required claims missing");
            if (!"ADMIN".equals(claims.get("role", String.class))) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator role required"); return; }
        } catch (Exception ex) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing, invalid, or expired bearer token"); return; }
        chain.doFilter(request, response);
    }
}
