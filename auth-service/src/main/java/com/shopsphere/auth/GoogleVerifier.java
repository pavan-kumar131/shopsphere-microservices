package com.shopsphere.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Validates Google's signed ID token; never trusts email/role fields sent separately by a browser. */
@Component
public class GoogleVerifier {
    public record Identity(String subject, String email, String name) {}
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private final String clientId;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public GoogleVerifier(@Value("${app.google-client-id:}") String clientId,
                          @Value("${app.google-jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri) throws Exception {
        this.clientId = clientId;
        URL url = URI.create(jwksUri).toURL();
        JWKSource<SecurityContext> keys = JWKSourceBuilder.<SecurityContext>create(url).build();
        this.processor = new DefaultJWTProcessor<>();
        this.processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
    }

    public Identity verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured: set GOOGLE_CLIENT_ID");
        }
        try {
            JWTClaimsSet c = processor.process(idToken, null);
            if (!ISSUERS.contains(c.getIssuer()) || c.getAudience() == null || !c.getAudience().contains(clientId)) throw invalid();
            String email = c.getStringClaim("email");
            if (c.getSubject() == null || email == null || !Boolean.TRUE.equals(c.getBooleanClaim("email_verified"))) throw invalid();
            return new Identity(c.getSubject(), email.trim().toLowerCase(java.util.Locale.ROOT), c.getStringClaim("name"));
        } catch (ParseException | BadJOSEException | JOSEException e) { throw invalid(); }
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google ID token is invalid or expired"); }
}
