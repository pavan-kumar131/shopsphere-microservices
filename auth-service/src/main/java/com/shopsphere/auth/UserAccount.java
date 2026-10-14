package com.shopsphere.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 20)
    private String role = "CUSTOMER";
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected UserAccount() {}
    public UserAccount(String email, String passwordHash) {
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.role = "CUSTOMER";
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
}
