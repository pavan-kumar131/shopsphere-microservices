package com.shopsphere.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${security.jwt.secret}") String base64Secret,
                      @Value("${security.jwt.expiration-minutes:15}") long expirationMinutes) {
        byte[] decoded = Base64.getDecoder().decode(base64Secret);
        if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(decoded);
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(UserAccount user) {
        return issue(user.getId().toString(), user.getEmail(), user.getRole());
    }

    public String issue(String userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
            .signWith(key)
            .compact();
    }
}
