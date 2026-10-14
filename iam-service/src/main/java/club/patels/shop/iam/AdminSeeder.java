package club.patels.shop.iam;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Admin allow-list. Only the emails in ADMIN_EMAILS (comma separated) are admins on first start.
 * Nobody can become admin through signup. Admins still sign in with Google, so turn on
 * 2-Step Verification on those Google accounts: that is the MFA.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService users;
    private final String adminEmails;

    public AdminSeeder(UserService users, @Value("${app.admin-emails:}") String adminEmails) {
        this.users = users;
        this.adminEmails = adminEmails;
    }

    @Override
    public void run(ApplicationArguments args) {
        Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .forEach(email -> {
                    users.ensureAdmin(email);
                    log.info("Admin allow-list: {} can sign in on the admin site", UserService.normalize(email));
                });
    }
}
