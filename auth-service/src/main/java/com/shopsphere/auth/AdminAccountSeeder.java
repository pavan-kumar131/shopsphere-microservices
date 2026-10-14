package com.shopsphere.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminAccountSeeder {
    @Bean ApplicationRunner seedAdmin(UserAccountRepository users, PasswordEncoder encoder,
            @Value("${app.admin.email:admin@shopsphere.local}") String email,
            @Value("${app.admin.password:AdminDemo123!}") String password) {
        return args -> {
            String normalized = email.trim().toLowerCase();
            if (!users.existsByEmailIgnoreCase(normalized)) {
                UserAccount admin = new UserAccount(normalized, encoder.encode(password));
                admin.setRole("ADMIN");
                users.save(admin);
                System.out.println("Local demo administrator seeded: " + normalized + " (change the password outside local development)");
            }
        };
    }
}
