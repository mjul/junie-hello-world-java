package com.example.hello.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(name = "uk_provider_external_id", columnNames = {"provider", "external_id"})
)
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String provider; // e.g., AZURE or GITHUB

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
