package club.patels.shop.iam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only (called through Feign by auth-service and profile-service).
 * The gateway has no route to /internal/**, so browsers cannot reach this.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    public record CreateUserRequest(@NotBlank @Email String email, @NotEmpty List<Role> roles) {}

    private final UserService service;

    public InternalUserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDto> byEmail(@RequestParam String email) {
        return service.findByEmail(email)
                .map(u -> ResponseEntity.ok(UserDto.of(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        if (request.roles().contains(Role.ADMIN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN_NOT_ALLOWED",
                    "Admins come from ADMIN_EMAILS or the admin API, never from signup");
        }
        Set<Role> roles = request.roles().stream().collect(Collectors.toSet());
        try {
            return UserDto.of(service.createIfAbsent(request.email(), roles));
        } catch (DataIntegrityViolationException race) {
            // two signups for the same email at the same moment: the other one won
            return service.findByEmail(request.email()).map(UserDto::of).orElseThrow(() -> race);
        }
    }
}
