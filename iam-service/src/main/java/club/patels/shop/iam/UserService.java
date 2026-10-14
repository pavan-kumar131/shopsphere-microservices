package club.patels.shop.iam;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return users.findByEmail(normalize(email));
    }

    /** Idempotent: returns the existing user untouched if the email is already registered. */
    @Transactional
    public UserAccount createIfAbsent(String email, Set<Role> roles) {
        return users.findByEmail(normalize(email))
                .orElseGet(() -> users.save(new UserAccount(normalize(email), roles)));
    }

    /** Used for the allow-listed admins: creates the account or adds the ADMIN role. */
    @Transactional
    public void ensureAdmin(String email) {
        UserAccount user = users.findByEmail(normalize(email))
                .orElseGet(() -> new UserAccount(normalize(email), Set.of(Role.ADMIN)));
        user.getRoles().add(Role.ADMIN);
        user.setActive(true);
        users.save(user);
    }
}
