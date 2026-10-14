package com.shopsphere.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserAccountRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 72) String password, String role) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String requestedRole = request.role() == null ? "CUSTOMER" : request.role().trim().toUpperCase();
        if (!requestedRole.equals("CUSTOMER") && !requestedRole.equals("SELLER")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Public registration supports CUSTOMER or SELLER only");
        UserAccount newUser = new UserAccount(email, encoder.encode(request.password()));
        newUser.setRole(requestedRole);
        UserAccount user = users.save(newUser);
        return Map.of("id", user.getId(), "email", user.getEmail(), "role", user.getRole());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return Map.of("accessToken", jwt.issue(user), "tokenType", "Bearer",
                      "userId", user.getId(), "email", user.getEmail(), "role", user.getRole(),
                      "expiresInSeconds", 900);
    }
}
