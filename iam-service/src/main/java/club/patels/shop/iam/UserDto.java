package club.patels.shop.iam;

import java.util.List;

public record UserDto(Long id, String email, List<String> roles, boolean active) {

    static UserDto of(UserAccount u) {
        return new UserDto(u.getId(), u.getEmail(),
                u.getRoles().stream().map(Enum::name).sorted().toList(), u.isActive());
    }
}
