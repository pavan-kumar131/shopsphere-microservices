package com.shopsphere.product;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalServiceVerifier {
    private final byte[] expected;
    public InternalServiceVerifier(@Value("${security.internal-token}") String token) {
        if (token == null || token.length() < 32) throw new IllegalArgumentException("INTERNAL_SERVICE_TOKEN must be at least 32 characters");
        expected = token.getBytes(StandardCharsets.UTF_8);
    }
    public void require(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal service authentication required");
    }
}
