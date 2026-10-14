package com.shopsphere.product;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAdminVerifier {
    private final SecretKey key;
    public JwtAdminVerifier(@Value("${security.jwt.secret}") String base64Secret) {
        byte[] decoded = Base64.getDecoder().decode(base64Secret);
        if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(decoded);
    }
    public Claims requireClaims(String authorization) { return verifiedClaims(authorization); }
    public void requireSellerOrAdmin(String authorization) {
        String role = verifiedClaims(authorization).get("role", String.class);
        if (!"SELLER".equals(role) && !"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller or administrator access required");
    }
    public void requireAdmin(String authorization) {
        if (!"ADMIN".equals(verifiedClaims(authorization).get("role", String.class))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
    }
    private Claims verifiedClaims(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() < 20) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in first");
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(authorization.substring(7)).getPayload();
            if (claims.getSubject() == null || claims.getExpiration() == null || claims.getExpiration().getTime() <= System.currentTimeMillis() || claims.get("role", String.class) == null) throw new IllegalArgumentException("Required claims missing");
            return claims;
        } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token"); }
    }
}
