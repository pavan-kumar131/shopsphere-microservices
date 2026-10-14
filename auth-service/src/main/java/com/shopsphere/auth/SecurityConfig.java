package com.shopsphere.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    // Prevent Spring Boot from creating/logging a random in-memory default user.
    // Login is handled by AuthController; this database-backed service is not a public login flow.
    @Bean
    UserDetailsService userDetailsService(UserAccountRepository users) {
        return username -> users.findByEmailIgnoreCase(username.trim().toLowerCase())
            .map(account -> User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // JSON API learning scaffold; reassess if using cookie-based auth.
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
<<<<<<< HEAD
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/google", "/actuator/health", "/actuator/health/**").permitAll()
=======
                .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health").permitAll()
>>>>>>> origin/main
                .anyRequest().denyAll())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .build();
    }
}
