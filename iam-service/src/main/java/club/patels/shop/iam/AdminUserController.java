package club.patels.shop.iam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reached as /api/admin/users/** through the gateway, which already insists on an admin token.
 * Each method re-checks X-Roles anyway, so a mistake in gateway routing cannot expose it.
 */
@RestController
@RequestMapping("/iam/users")
public class AdminUserController {

    public record PageDto<T>(List<T> content, int page, int size, long totalElements) {}
    public record RolesRequest(@NotEmpty List<Role> roles) {}
    public record ActiveRequest(boolean active) {}

    private final UserRepository users;

    public AdminUserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public PageDto<UserDto> list(@RequestHeader(value = "X-Roles", required = false) String roles,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        requireAdmin(roles);
        Page<UserAccount> result = users.findAll(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending()));
        return new PageDto<>(result.map(UserDto::of).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements());
    }

    @PutMapping("/{id}/roles")
    @Transactional
    public UserDto setRoles(@RequestHeader(value = "X-Roles", required = false) String roles,
                            @RequestHeader(value = "X-User-Id", required = false) String caller,
                            @PathVariable Long id,
                            @Valid @RequestBody RolesRequest request) {
        requireAdmin(roles);
        UserAccount user = load(id);
        Set<Role> newRoles = request.roles().stream().collect(Collectors.toSet());
        if (id.toString().equals(caller) && !newRoles.contains(Role.ADMIN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_LOCKOUT", "You cannot remove your own ADMIN role");
        }
        user.setRoles(newRoles);
        return UserDto.of(users.save(user));
    }

    @PutMapping("/{id}/active")
    @Transactional
    public UserDto setActive(@RequestHeader(value = "X-Roles", required = false) String roles,
                             @RequestHeader(value = "X-User-Id", required = false) String caller,
                             @PathVariable Long id,
                             @RequestBody ActiveRequest request) {
        requireAdmin(roles);
        if (id.toString().equals(caller) && !request.active()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_LOCKOUT", "You cannot disable your own account");
        }
        UserAccount user = load(id);
        user.setActive(request.active());
        return UserDto.of(users.save(user));
    }

    private UserAccount load(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
    }

    static void requireAdmin(String rolesHeader) {
        boolean admin = rolesHeader != null
                && Arrays.stream(rolesHeader.split(",")).map(String::trim).anyMatch("ADMIN"::equals);
        if (!admin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin only");
        }
    }
}
