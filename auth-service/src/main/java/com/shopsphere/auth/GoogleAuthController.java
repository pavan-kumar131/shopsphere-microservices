package com.shopsphere.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {
    private static final Set<String> SITES = Set.of("customer", "seller", "admin");
    public record GoogleRequest(@NotBlank String idToken, @NotBlank String site) {}
    private final GoogleVerifier google;
    private final IamClient iam;
    private final JwtService jwt;

    public GoogleAuthController(GoogleVerifier google, IamClient iam, JwtService jwt) {
        this.google = google; this.iam = iam; this.jwt = jwt;
    }

    @PostMapping("/google")
    public Map<String, Object> google(@Valid @RequestBody GoogleRequest request) {
        String site = request.site().trim().toLowerCase(Locale.ROOT);
        if (!SITES.contains(site)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "site must be customer, seller or admin");
        GoogleVerifier.Identity identity = google.verify(request.idToken());
        IamClient.IamUser user = iam.findByEmail(identity.email());
        if (user == null) {
            if (!site.equals("customer")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not enrolled for this site; sign up as a customer first or contact an administrator");
            user = iam.create(new IamClient.CreateUserRequest(identity.email(), List.of("CUSTOMER")));
        }
        if (user == null || !user.active()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled or unavailable");
        String requiredRole = site.toUpperCase(Locale.ROOT);
        if (user.roles() == null || !user.roles().contains(requiredRole)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not registered for the " + site + " site");
        return Map.of("accessToken", jwt.issue(user.id().toString(), user.email(), requiredRole),
                      "tokenType", "Bearer", "userId", user.id().toString(), "email", user.email(),
                      "role", requiredRole, "expiresInSeconds", 900, "authProvider", "google");
    }
}
