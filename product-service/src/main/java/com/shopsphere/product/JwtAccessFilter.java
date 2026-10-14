package com.shopsphere.product;

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

/** Validates bearer JWTs at the HTTP boundary. Controller-level checks still enforce resource ownership. */
@Component
public class JwtAccessFilter extends OncePerRequestFilter {
  private final SecretKey key;
  public JwtAccessFilter(@Value("${security.jwt.secret}") String secret) {
    byte[] decoded = Base64.getDecoder().decode(secret);
    if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
    this.key = Keys.hmacShaKeyFor(decoded);
  }
  @Override protected boolean shouldNotFilter(HttpServletRequest request) {
    String path=request.getRequestURI();
    return path.equals("/actuator/health") || path.startsWith("/actuator/health/") || path.startsWith("/internal/") ||
      (path.startsWith("/api/products") && "GET".equals(request.getMethod()));
  }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    Claims claims;
    try {
      String header = request.getHeader("Authorization");
      if (header == null || !header.startsWith("Bearer ")) throw new IllegalArgumentException("Missing bearer token");
      claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
      if (claims.getSubject() == null || claims.getExpiration() == null || claims.getExpiration().getTime() <= System.currentTimeMillis() || claims.get("role", String.class) == null) throw new IllegalArgumentException("Required claims missing");
    } catch (Exception ex) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing, invalid, or expired bearer token"); return; }
    String role = claims.get("role", String.class);
    String method = request.getMethod();
    String path = request.getRequestURI();
    if (path.startsWith("/api/products") && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))
        && !"ADMIN".equals(role) && !("POST".equals(method) && "SELLER".equals(role))) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient role"); return;
    }
    chain.doFilter(request, response);
  }

}
