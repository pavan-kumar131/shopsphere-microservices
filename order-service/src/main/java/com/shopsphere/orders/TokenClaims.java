package com.shopsphere.orders;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TokenClaims {
    private final SecretKey key;

    public TokenClaims(@Value("${shopsphere.jwt-secret}") String secret) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must be base64 for at least 32 random bytes");
        this.key = Keys.hmacShaKeyFor(decoded);
    }

    public Claims require(String auth) {
        try {
            if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalArgumentException("Missing bearer token");
            io.jsonwebtoken.Claims c = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(auth.substring(7)).getPayload();
            String role = c.get("role", String.class);
            if (c.getSubject() == null || role == null) throw new IllegalArgumentException("Required claims missing");
            return new Claims(Long.valueOf(c.getSubject()), role);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid bearer token required");
        }
    }

    public record Claims(Long userId, String role) {}
}
